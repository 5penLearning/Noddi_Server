package com._penLearning.Noddi.domain.notification.event;

import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCreateService;
import com._penLearning.Noddi.domain.project.event.ProjectInviteCreatedEvent;
import com._penLearning.Noddi.domain.team.event.TeamInviteCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteNotificationEventHandlerTest {

    @Mock private NotificationCreateService notificationCreateService;
    @Mock private NotificationMessageFactory messageFactory;

    @Test
    void createsTeamInviteNotificationWithTeamNavigationData() {
        // Given: 팀 초대가 저장된 뒤 초대 생성 이벤트가 발행됐다.
        InviteNotificationEventHandler handler = handler();
        TeamInviteCreatedEvent event = new TeamInviteCreatedEvent(
                300L,
                1L,
                "노디프로젝트",
                2L,
                "마케팅팀",
                "홍길동",
                10L
        );
        when(messageFactory.createTeamInviteMessage(
                "노디프로젝트",
                "마케팅팀",
                "홍길동"
        )).thenReturn("팀 초대 알림 문구");

        // When: 공통 알림 핸들러가 팀 초대 이벤트를 처리한다.
        handler.handleTeamInviteCreated(event);

        // Then: 초대받은 사용자에게 teamInviteId와 프로젝트·팀 이동 정보를 저장한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.TEAM_INVITE,
                NotificationReferenceType.TEAM_INVITE,
                300L,
                1L,
                2L,
                "팀 초대 알림 문구"
        );
    }

    @Test
    void createsProjectInviteNotificationWithoutTeamId() {
        // Given: 프로젝트 초대가 저장된 뒤 초대 생성 이벤트가 발행됐다.
        InviteNotificationEventHandler handler = handler();
        ProjectInviteCreatedEvent event = new ProjectInviteCreatedEvent(
                400L,
                1L,
                "노디프로젝트",
                "홍길동",
                10L
        );
        when(messageFactory.createProjectInviteMessage(
                "노디프로젝트",
                "홍길동"
        )).thenReturn("프로젝트 초대 알림 문구");

        // When: 공통 알림 핸들러가 프로젝트 초대 이벤트를 처리한다.
        handler.handleProjectInviteCreated(event);

        // Then: 프로젝트 초대에는 팀이 없으므로 teamId를 null로 저장한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.PROJECT_INVITE,
                NotificationReferenceType.PROJECT_INVITE,
                400L,
                1L,
                null,
                "프로젝트 초대 알림 문구"
        );
    }

    private InviteNotificationEventHandler handler() {
        return new InviteNotificationEventHandler(
                notificationCreateService,
                messageFactory
        );
    }
}
