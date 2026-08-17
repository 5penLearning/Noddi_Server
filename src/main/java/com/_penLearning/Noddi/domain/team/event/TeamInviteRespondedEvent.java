package com._penLearning.Noddi.domain.team.event;

/**
 * 팀 초대 수락 또는 거절 처리가 정상적으로 완료됐음을 알린다.
 *
 * 알림함에서는 수락과 거절을 구분하지 않고
 * 해당 초대 알림을 숨기므로 inviteId만 전달한다.
 */
public record TeamInviteRespondedEvent(
        Long inviteId
) {
}