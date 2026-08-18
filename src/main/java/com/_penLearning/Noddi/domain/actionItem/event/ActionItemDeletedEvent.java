package com._penLearning.Noddi.domain.actionItem.event;

/**
 * Action Item이 삭제돼 기존 배정 알림이 더 이상 유효하지 않음을 알린다.
 */
public record ActionItemDeletedEvent(
        Long actionItemId
) {
}