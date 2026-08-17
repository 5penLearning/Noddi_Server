package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishType;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
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


// 대상 팀 구성원이 AI 답변을 수정하거나 AI 최종 실패 후 직접 답변한다.
// QaAnswer에는 현재 최종본을, QaAnswerRevision에는 버전별 내용을 누적 보존한다.
@Service
@RequiredArgsConstructor
public class QaAnswerUpdateService {

    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerRevisionRepository qaAnswerRevisionRepository;
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

            // 시스템 안내문은 답변 이력이 아니므로, 팀원의 실제 답변을 1번 버전으로 시작한다.
            qaAnswerRevisionRepository.save(QaAnswerRevision.createHumanRevision(
                    answer,
                    1,
                    request.getContent(),
                    reviser
            ));
            publishType = QaAnswerPublishType.TEAM_PROVIDED;
        } else if (question.getStatus() == QaStatus.ANSWERED) {
            // 네트워크 재전송 등으로 동일한 내용이 들어오면 이력·알림을 중복 생성하지 않는다.
            if (Objects.equals(answer.getContent(), request.getContent())) {
                return QaResponseDto.ReviseAnswer.from(answer);
            }

            QaAnswerRevision lastRevision = qaAnswerRevisionRepository
                    .findTopByAnswerOrderByVersionNumberDesc(answer)
                    .orElseThrow(() -> new GeneralException(QaErrorCode.REVISION_HISTORY_NOT_FOUND));

            qaAnswerRevisionRepository.save(QaAnswerRevision.createHumanRevision(
                    answer,
                    lastRevision.getVersionNumber() + 1,
                    request.getContent(),
                    reviser
            ));
            answer.revise(request.getContent(), reviser);
            publishType = QaAnswerPublishType.ANSWER_REVISED;
        } else {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }

        // AI 출처는 현재 수정본에서는 노출하지 않지만, AI 원문 이력에서 보여주기 위해 삭제하지 않는다.
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
