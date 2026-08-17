package com._penLearning.Noddi.domain.notification.event;

import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCreateService;
import com._penLearning.Noddi.domain.qa.event.QaAiFinalFailureEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class QaNotificationEventHandler {

    private final NotificationCreateService notificationCreateService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationMessageFactory notificationMessageFactory;

    /**
     * AI 답변, 팀원 직접 답변, 담당자 수정이 DB에 정상 반영된 뒤 호출된다.
     *
     * 원래 Q&A 트랜잭션이 커밋된 후 알림을 저장해야
     * 답변 저장이 롤백됐는데 알림만 남는 상황을 막을 수 있다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAnswerPublished(QaAnswerPublishedEvent event) {
        Team targetTeam = getTeamOrThrow(event.targetTeamId());

        Long projectId = targetTeam.getProject().getProjectId();
        Long teamId = targetTeam.getTeamId();

        String projectName = targetTeam.getProject().getName();
        String teamName = targetTeam.getName();
        switch (event.publishType()) {
            case AI_GENERATED -> {
                /*
                 * 질문자에게는 답변이 등록됐다는 사실을 알린다.
                 * AI 실패나 검토 필요 여부는 질문자에게 노출하지 않는다.
                 */
                notificationCreateService.createNotification(
                        event.questionerId(),
                        NotificationType.QA_ANSWERED,
                        NotificationReferenceType.QA_QUESTION,
                        event.questionId(),
                        projectId,
                        teamId,
                        notificationMessageFactory.createQaMessage(NotificationType.QA_ANSWERED, projectName, teamName)
                );

                /*
                 * 대상 팀원에게는 AI가 대신 답변했으니
                 * 추후 내용을 검토하라는 알림을 각각 생성한다.
                 */
                createNotificationsForTargetTeam(
                        targetTeam,
                        NotificationType.QA_AI_REVIEW_REQUIRED,
                        event.questionId(),
                        notificationMessageFactory.createQaMessage(NotificationType.QA_AI_REVIEW_REQUIRED, projectName, teamName)
                );
            }

            case TEAM_PROVIDED -> {
                /*
                 * 대상 팀원이 직접 작성한 답변이 등록된 경우다.
                 *
                 * 질문자가 직접 답변 작성자와 동일한 특수 상황에서는
                 * 자신이 수행한 작업에 대한 알림을 생성하지 않는다.
                 */
                if (!Objects.equals(event.questionerId(), event.actorId())) {
                    notificationCreateService.createNotification(
                            event.questionerId(),
                            NotificationType.QA_ANSWERED,
                            NotificationReferenceType.QA_QUESTION,
                            event.questionId(),
                            projectId,
                            teamId,
                            notificationMessageFactory.createQaMessage(NotificationType.QA_ANSWERED, projectName, teamName)
                    );
                }
            }

            case ANSWER_REVISED -> {
                /*
                 * 동일 답변이 여러 번 수정되더라도 질문자가 아직 읽지 않았다면
                 * 알림 레코드를 계속 추가하지 않고 기존 알림을 최신 상태로 갱신한다.
                 */
                if (!Objects.equals(event.questionerId(), event.actorId())) {
                    notificationCreateService.createOrUpdateUnread(
                            event.questionerId(),
                            NotificationType.QA_ANSWER_REVISED,
                            NotificationReferenceType.QA_QUESTION,
                            event.questionId(),
                            projectId,
                            teamId,
                            notificationMessageFactory.createQaMessage(NotificationType.QA_ANSWER_REVISED, projectName, teamName)
                    );
                }
            }
        }
    }

    /**
     * AI 자동 재시도가 모두 실패한 뒤 호출된다.
     *
     * 질문자에게는 AI 실패를 직접 노출하지 않고 답변 대기 상태만 안내한다.
     * 실제 AI 실패 및 직접 답변 요청은 대상 팀원에게만 전달한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAiFinalFailure(QaAiFinalFailureEvent event) {
        Team targetTeam = getTeamOrThrow(event.targetTeamId());

        Long projectId = targetTeam.getProject().getProjectId();
        Long teamId = targetTeam.getTeamId();

        String projectName = targetTeam.getProject().getName();
        String teamName = targetTeam.getName();

        // 질문자에게는 내부 AI 실패 상태를 숨기고 일반적인 답변 대기만 안내한다.
        notificationCreateService.createNotification(
                event.questionerId(),
                NotificationType.QA_ANSWER_WAITING,
                NotificationReferenceType.QA_QUESTION,
                event.questionId(),
                projectId,
                teamId,
                notificationMessageFactory.createQaMessage(NotificationType.QA_ANSWER_WAITING, projectName, teamName)
        );

        // 대상 팀원에게는 직접 답변이 필요하다는 알림을 각각 생성한다.
        createNotificationsForTargetTeam(
                targetTeam,
                NotificationType.QA_AI_FAILED,
                event.questionId(),
                notificationMessageFactory.createQaMessage(NotificationType.QA_AI_FAILED, projectName, teamName)
        );
    }

    /**
     * 현재 대상 팀에 소속된 모든 사용자에게 같은 Q&A 알림을 생성한다.
     *
     * TeamMember가 아닌 User를 수신자로 저장해야 하므로
     * 각 팀 멤버에서 userId를 꺼내 알림 생성 서비스에 전달한다.
     */
    private void createNotificationsForTargetTeam(
            Team targetTeam,
            NotificationType type,
            Long questionId,
            String message
    ) {
        List<TeamMember> targetTeamMembers =
                teamMemberRepository.findAllByTeamWithUser(targetTeam);

        Long projectId = targetTeam.getProject().getProjectId();
        Long teamId = targetTeam.getTeamId();

        for (TeamMember teamMember : targetTeamMembers) {
            notificationCreateService.createNotification(
                    teamMember.getUser().getUserId(),
                    type,
                    NotificationReferenceType.QA_QUESTION,
                    questionId,
                    projectId,
                    teamId,
                    message
            );
        }
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new GeneralException(TeamErrorCode.TEAM_NOT_FOUND)
                );
    }
}