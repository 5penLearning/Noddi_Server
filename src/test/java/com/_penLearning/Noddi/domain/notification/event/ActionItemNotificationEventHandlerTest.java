package com._penLearning.Noddi.domain.notification.event;

import com._penLearning.Noddi.domain.actionItem.event.ActionItemAssigneeChangedEvent;
import com._penLearning.Noddi.domain.actionItem.event.ActionItemDeletedEvent;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCommandService;
import com._penLearning.Noddi.domain.notification.service.NotificationCreateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionItemNotificationEventHandlerTest {

    @Mock private NotificationCommandService notificationCommandService;
    @Mock private NotificationCreateService notificationCreateService;
    @Mock private NotificationMessageFactory messageFactory;

    @Test
    void hidesPreviousNotificationAndCreatesNotificationForNewAssignee() {
        // Given: 사용자 20이 Action Item 담당자를 사용자 10으로 지정했다.
        ActionItemNotificationEventHandler handler = handler();
        ActionItemAssigneeChangedEvent event = event(10L, 20L);
        when(messageFactory.createActionItemAssignedMessage(
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("Action Item 배정 알림");

        // When: 담당자 변경 이벤트를 처리한다.
        handler.handleAssigneeChanged(event);

        // Then: 기존 담당자의 만료된 알림을 먼저 정리한다.
        verifyResolvedAssignmentNotification();

        // 새 담당자에게 홈 이동에 필요한 프로젝트·팀 정보와 내부 참조를 저장한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.ACTION_ITEM_ASSIGNED,
                NotificationReferenceType.ACTION_ITEM,
                500L,
                1L,
                2L,
                "Action Item 배정 알림"
        );
    }

    @Test
    void createsNotificationForAiAssignedActionItem() {
        // Given: AI 자동 처리에는 요청 사용자가 없으므로 actorId가 null이다.
        ActionItemNotificationEventHandler handler = handler();
        ActionItemAssigneeChangedEvent event = event(10L, null);
        when(messageFactory.createActionItemAssignedMessage(
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("AI Action Item 배정 알림");

        // When: AI가 담당자를 정확하게 식별한 Action Item 이벤트를 처리한다.
        handler.handleAssigneeChanged(event);

        // Then: actorId가 없더라도 담당자에게 정상적으로 알림을 생성한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.ACTION_ITEM_ASSIGNED,
                NotificationReferenceType.ACTION_ITEM,
                500L,
                1L,
                2L,
                "AI Action Item 배정 알림"
        );
    }

    @Test
    void doesNotCreateNotificationForSelfAssignment() {
        // Given: 사용자가 자신에게 Action Item을 할당했다.
        ActionItemNotificationEventHandler handler = handler();

        // When: assigneeId와 actorId가 같은 이벤트를 처리한다.
        handler.handleAssigneeChanged(event(10L, 10L));

        // Then: 재배정 전 알림 정리는 수행하되 자기 자신에게 새 알림은 만들지 않는다.
        verifyResolvedAssignmentNotification();
        verify(notificationCreateService, never()).createNotification(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void hidesPreviousNotificationWithoutCreatingOneWhenUnassigned() {
        // Given: Action Item 담당자가 해제돼 새 assigneeId가 없다.
        ActionItemNotificationEventHandler handler = handler();

        // When: 담당 해제 이벤트를 처리한다.
        handler.handleAssigneeChanged(event(null, 20L));

        // Then: 기존 배정 알림만 숨기고 새 알림은 생성하지 않는다.
        verifyResolvedAssignmentNotification();
        verify(notificationCreateService, never()).createNotification(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void hidesAssignmentNotificationAfterActionItemDeletion() {
        // Given: 배정 알림이 존재하는 Action Item이 삭제됐다.
        ActionItemNotificationEventHandler handler = handler();

        // When: 삭제 이벤트를 처리한다.
        handler.handleDeleted(new ActionItemDeletedEvent(500L));

        // Then: 삭제된 Action Item으로 이동하는 기존 알림을 숨긴다.
        verifyResolvedAssignmentNotification();
    }

    private ActionItemNotificationEventHandler handler() {
        return new ActionItemNotificationEventHandler(
                notificationCommandService,
                notificationCreateService,
                messageFactory
        );
    }

    private ActionItemAssigneeChangedEvent event(
            Long assigneeId,
            Long actorId
    ) {
        return new ActionItemAssigneeChangedEvent(
                500L,
                1L,
                "노디프로젝트",
                2L,
                "마케팅팀",
                assigneeId,
                actorId
        );
    }

    private void verifyResolvedAssignmentNotification() {
        verify(notificationCommandService)
                .resolveReferenceNotifications(
                        NotificationReferenceType.ACTION_ITEM,
                        500L,
                        Set.of(NotificationType.ACTION_ITEM_ASSIGNED)
                );
    }
}
