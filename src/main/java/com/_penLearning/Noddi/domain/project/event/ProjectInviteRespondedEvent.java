package com._penLearning.Noddi.domain.project.event;

/**
 * 프로젝트 초대 수락 또는 거절 처리가 정상적으로 완료됐음을 알린다.
 */
public record ProjectInviteRespondedEvent(
        Long inviteId
) {
}