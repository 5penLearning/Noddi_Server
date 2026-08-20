package com._penLearning.Noddi.domain.project.event;

/**
 * 프로젝트 초대가 DB에 저장됐음을 알리는 도메인 이벤트다.
 */
public record ProjectInviteCreatedEvent(
        Long inviteId,
        Long projectId,
        String projectName,
        String inviterName,
        Long inviteeId
) {
}