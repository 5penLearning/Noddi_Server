package com._penLearning.Noddi.domain.team.event;

/**
 * 팀 초대가 DB에 저장됐음을 알리는 도메인 이벤트다.
 *
 * 알림 도메인이 원본 엔티티를 다시 조회하지 않아도 되도록
 * 알림 생성과 문구 작성에 필요한 값을 이벤트 발생 시점에 전달한다.
 */
public record TeamInviteCreatedEvent(
        Long inviteId,
        Long projectId,
        String projectName,
        Long teamId,
        String teamName,
        String inviterName,
        Long inviteeId
) {
}