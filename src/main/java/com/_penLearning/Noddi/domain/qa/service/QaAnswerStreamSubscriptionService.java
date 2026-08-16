package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaAnswerStreamSubscriptionService {

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAnswerRepository qaAnswerRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final QaAnswerStreamService qaAnswerStreamService;

    /**
     * 사용자가 특정 질문의 AI 답변 스트림을 구독한다.
     *
     * SSE 연결을 생성하기 전에 다음 사항을 확인
     *
     * 1. 요청한 사용자가 실제로 존재하는가?
     * 2. 구독하려는 질문이 존재하는가?
     * 3. 사용자가 질문이 속한 프로젝트의 멤버인가?
     * 4. 이미 답변이 완료됐다면 저장된 답변은 무엇인가?
     */
    public SseEmitter subscribe(
            Long requesterId,
            Long questionId
    ) {
        User requester = getUserOrThrow(requesterId);
        QaQuestion question = getQuestionOrThrow(questionId);

        validateProjectMember(requester, question);

        /*
         * PENDING 또는 PROCESSING 상태에서는 아직 저장된 답변이 없으므로
         * answerId와 answerContent를 null로 전달한다.
         */
        Long answerId = null;
        String answerContent = null;

        /*
         * ANSWERED 상태라면 메모리 스트림이 이미 사라졌거나 서버가 재시작됐어도
         * DB에 저장된 최종 답변을 즉시 전달할 수 있어야 한다.
         */
        if (question.getStatus() == QaStatus.ANSWERED) {
            QaAnswer answer = qaAnswerRepository.findByQuestion(question)
                    .orElseThrow(() ->
                            new GeneralException(QaErrorCode.ANSWER_NOT_FOUND)
                    );

            answerId = answer.getAnswerId();
            answerContent = answer.getContent();
        }

        /*
         * 권한 검증과 현재 상태 조회를 마친 뒤 실제 SSE 연결을 생성한다.
         *
         * QaAnswerStreamService는 DB를 직접 조회하지 않고 전달받은 상태만으로
         * 연결과 이벤트 전송을 관리한다.
         */
        return qaAnswerStreamService.subscribe(
                question.getQuestionId(),
                question.getStatus(),
                answerId,
                answerContent
        );
    }

    private User getUserOrThrow(Long requesterId) {
        return userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new GeneralException(UserErrorCode.USER_NOT_FOUND)
                );
    }

    private QaQuestion getQuestionOrThrow(Long questionId) {
        /*
         * findByIdWithTeam을 사용해 권한 검증에 필요한 targetTeam을
         * 질문과 함께 조회한다.
         */
        return qaQuestionRepository.findByIdWithTeam(questionId)
                .orElseThrow(() ->
                        new GeneralException(QaErrorCode.QUESTION_NOT_FOUND)
                );
    }

    private void validateProjectMember(
            User requester,
            QaQuestion question
    ) {
        /*
         * 질문 대상 팀의 팀원만 구독할 수 있는 것이 아니다.
         *
         * 합의한 정책에 따라 같은 프로젝트의 모든 멤버가 Q&A 피드를
         * 볼 수 있으므로 SSE 답변도 같은 프로젝트 멤버라면 구독할 수 있다.
         */
        boolean isProjectMember =
                projectMemberRepository.existsByProjectAndUser(
                        question.getTargetTeam().getProject(),
                        requester
                );

        if (!isProjectMember) {
            throw new GeneralException(QaErrorCode.NOT_PROJECT_MEMBER);
        }
    }
}