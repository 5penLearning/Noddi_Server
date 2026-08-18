package com._penLearning.Noddi.domain.actionItem.event;

/**
 * Action Item의 담당자가 새로 지정되거나 변경·해제됐음을 알린다.
 *
 * assigneeId:
 * - 담당자 지정: 사용자 ID
 * - 담당자 해제: null
 *
 * actorId:
 * - 사용자가 직접 생성·수정: 요청자 ID
 * - AI 자동 생성: null
 */
public record ActionItemAssigneeChangedEvent(
        Long actionItemId,

        Long projectId,
        String projectName,

        Long teamId,
        String teamName,

        Long assigneeId,
        //자기 자신에게 할당한 경우를 구분하기 위한 필드
        Long actorId
) {
}