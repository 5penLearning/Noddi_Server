package com._penLearning.Noddi.domain.notification.event;

import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCommandService;
import com._penLearning.Noddi.domain.notification.service.NotificationCreateService;
import com._penLearning.Noddi.domain.project.event.ProjectInviteCreatedEvent;
import com._penLearning.Noddi.domain.project.event.ProjectInviteRespondedEvent;
import com._penLearning.Noddi.domain.team.event.TeamInviteCreatedEvent;
import com._penLearning.Noddi.domain.team.event.TeamInviteRespondedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class InviteNotificationEventHandler {

    private final NotificationCreateService notificationCreateService;
    private final NotificationMessageFactory notificationMessageFactory;
    private final NotificationCommandService notificationCommandService;

    /**
     * 팀 초대가 DB에 정상적으로 저장된 이후
     * 초대받은 사용자의 공통 알림함에 팀 초대 알림을 생성한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTeamInviteCreated(
            TeamInviteCreatedEvent event
    ) {
        String message = notificationMessageFactory.createTeamInviteMessage(
                event.projectName(),
                event.teamName(),
                event.inviterName()
        );

        notificationCreateService.createNotification(
                event.inviteeId(),
                NotificationType.TEAM_INVITE,
                NotificationReferenceType.TEAM_INVITE,

                // 자세히보기 또는 초대 응답에 필요한 TeamInvite의 ID
                event.inviteId(),

                // 프로젝트·팀별 표시와 프론트 라우팅에 사용
                event.projectId(),
                event.teamId(),
                message
        );
    }

    /**
     * 팀 초대 응답 트랜잭션이 커밋되면
     * 더 이상 행동할 필요가 없는 팀 초대 알림을 숨긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTeamInviteResponded(
            TeamInviteRespondedEvent event
    ) {
        notificationCommandService.resolveReferenceNotifications(
                NotificationReferenceType.TEAM_INVITE,
                event.inviteId(),
                Set.of(NotificationType.TEAM_INVITE)
        );
    }

    /**
     * 프로젝트 초대가 DB에 정상적으로 저장된 이후
     * 초대받은 사용자의 공통 알림함에 프로젝트 초대 알림을 생성한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProjectInviteCreated(
            ProjectInviteCreatedEvent event
    ) {
        String message = notificationMessageFactory.createProjectInviteMessage(
                event.projectName(),
                event.inviterName()
        );

        notificationCreateService.createNotification(
                event.inviteeId(),
                NotificationType.PROJECT_INVITE,
                NotificationReferenceType.PROJECT_INVITE,

                // 자세히보기 또는 초대 응답에 필요한 ProjectInvite의 ID
                event.inviteId(),

                // 프로젝트 초대는 아직 소속 팀이 없으므로 teamId는 null
                event.projectId(),
                null,
                message
        );
    }

    /**
     * 프로젝트 초대 응답 트랜잭션이 커밋되면
     * 더 이상 행동할 필요가 없는 프로젝트 초대 알림을 숨긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProjectInviteResponded(
            ProjectInviteRespondedEvent event
    ) {
        notificationCommandService.resolveReferenceNotifications(
                NotificationReferenceType.PROJECT_INVITE,
                event.inviteId(),
                Set.of(NotificationType.PROJECT_INVITE)
        );
    }
}