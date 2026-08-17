package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishType;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


// 대상 팀 구성원이 AI 답변을 수정한다.
// 수정 전 원문은 별도로 보관하지 않고 현재 답변과 마지막 수정자만 갱신한다.
@Service
@RequiredArgsConstructor
public class QaAnswerUpdateService {

    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;
    private final QaQuestionRepository qaQuestionRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public QaResponseDto.ReviseAnswer revise(
            Long answerId,
            Long reviserId,
            QaRequestDto.ReviseAnswer request
    ) {
        QaAnswer answer = qaAnswerRepository.findByIdWithQuestionAndTeamForUpdate(answerId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.ANSWER_NOT_FOUND));

        User reviser = userRepository.findById(reviserId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!teamMemberRepository.existsByTeamAndUser(answer.getQuestion().getTargetTeam(), reviser)) {
            throw new GeneralException(QaErrorCode.NOT_TARGET_TEAM_MEMBER);
        }

        QaQuestion question = qaQuestionRepository.findByIdWithLock(
                        answer.getQuestion().getQuestionId()
                )
                .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));

        QaAnswerPublishType publishType;
        if (question.getStatus() == QaStatus.MANUAL_REQUIRED) {
            answer.provideByTeam(request.getContent());
            question.completeManualAnswer();
            publishType = QaAnswerPublishType.TEAM_PROVIDED;
        } else if (question.getStatus() == QaStatus.ANSWERED) {
            // 네트워크 재전송 등으로 동일한 내용이 들어오면 수정·출처 삭제·알림 이벤트를 반복하지 않는다.
            if (Objects.equals(answer.getContent(), request.getContent())) {
                return QaResponseDto.ReviseAnswer.from(answer);
            }
            answer.revise(request.getContent(), reviser);
            publishType = QaAnswerPublishType.ANSWER_REVISED;
        } else {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }

        // 수정본은 AI 원문과 별개의 최종 답변이므로 기존 AI 인용 출처를 더 이상 노출하지 않는다.
        qaAnswerSourceRepository.deleteAllByAnswerId(answer.getAnswerId());
        eventPublisher.publishEvent(new QaAnswerPublishedEvent(
                question.getQuestionId(),
                answer.getAnswerId(),
                question.getQuestioner().getUserId(),
                question.getTargetTeam().getTeamId(),
                reviser.getUserId(),
                publishType
        ));
        return QaResponseDto.ReviseAnswer.from(answer);
    }
}
