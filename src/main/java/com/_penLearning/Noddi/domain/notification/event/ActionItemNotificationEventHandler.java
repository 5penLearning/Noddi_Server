package com._penLearning.Noddi.domain.notification.event;

import com._penLearning.Noddi.domain.actionItem.event.ActionItemAssigneeChangedEvent;
import com._penLearning.Noddi.domain.actionItem.event.ActionItemDeletedEvent;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCommandService;
import com._penLearning.Noddi.domain.notification.service.NotificationCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ActionItemNotificationEventHandler {

    private final NotificationCommandService notificationCommandService;
    private final NotificationCreateService notificationCreateService;
    private final NotificationMessageFactory notificationMessageFactory;

    /**
     * Action Item 담당자 변경 트랜잭션이 커밋된 뒤 실행한다.
     *
     * 처리 순서:
     * 1. 기존 담당자에게 생성됐던 알림 숨김
     * 2. 새 담당자가 없거나 자기 자신에게 할당한 경우 종료
     * 3. 새 담당자에게 배정 알림 생성
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAssigneeChanged(
            ActionItemAssigneeChangedEvent event
    ) {
        /*
         * 재배정·담당 해제 시 기존 담당자가 받은 알림이
         * 더 이상 유효하지 않으므로 먼저 숨긴다.
         *
         * 신규 생성이라 기존 알림이 없어도 빈 목록이 반환되므로
         * 별도 존재 여부 검사는 필요하지 않다.
         */
        notificationCommandService.resolveReferenceNotifications(
                NotificationReferenceType.ACTION_ITEM,
                event.actionItemId(),
                Set.of(NotificationType.ACTION_ITEM_ASSIGNED)
        );

        /*
         * 담당자가 해제된 경우에는 기존 알림만 숨기고 종료한다.
         */
        if (event.assigneeId() == null) {
            return;
        }

        /*
         * 사용자가 자신에게 할당한 경우 별도의 알림을 생성하지 않는다.
         *
         * AI 자동 생성은 actorId가 null이므로 이 조건에 해당하지 않아
         * 담당자에게 정상적으로 알림이 생성된다.
         */
        if (event.assigneeId().equals(event.actorId())) {
            return;
        }

        String message =
                notificationMessageFactory
                        .createActionItemAssignedMessage(
                                event.projectName(),
                                event.teamName()
                        );

        notificationCreateService.createNotification(
                event.assigneeId(),
                NotificationType.ACTION_ITEM_ASSIGNED,
                NotificationReferenceType.ACTION_ITEM,
                // 알림 수명주기 관리에 사용할 내부 참조
                event.actionItemId(),
                // 홈에서 선택할 프로젝트
                event.projectId(),
                // 응답에는 포함되지만 현재 프론트 이동에서는 사용하지 않음
                event.teamId(),
                message
        );
    }

    /**
     * Action Item 삭제가 커밋되면
     * 해당 Action Item을 참조하는 기존 알림을 숨긴다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDeleted(
            ActionItemDeletedEvent event
    ) {
        notificationCommandService.resolveReferenceNotifications(
                NotificationReferenceType.ACTION_ITEM,
                event.actionItemId(),
                Set.of(NotificationType.ACTION_ITEM_ASSIGNED)
        );
    }
}