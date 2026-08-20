package com._penLearning.Noddi.domain.project.dto;

import com._penLearning.Noddi.domain.project.entity.ProjectInvite;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

public class ProjectMemberResponseDto {

    // 프로젝트와 같은 조직에 속하면서 아직 초대 가능한 사용자 DTO
    @Getter
    @Builder
    public static class InviteCandidate {
        private Long userId;
        private String name;
        private String email;

        public static InviteCandidate from(User user) {
            return InviteCandidate.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        }
    }

    // 프로젝트 멤버 조회용 Dto
    @Getter
    @Builder
    public static class MemberInfo {
        private Long userId;
        private String name;
        private String email;
        private ProjectRole role;

        public static MemberInfo from(ProjectMember projectMember) {
            return MemberInfo.builder()
                    .userId(projectMember.getUser().getUserId())
                    .name(projectMember.getUser().getName())
                    .email(projectMember.getUser().getEmail())
                    .role(projectMember.getRole())
                    .build();
        }
    }

    // 내가 받은 초대장 조회용 Dto
    @Getter
    @Builder
    public static class InvitationInfo {
        private Long inviteId;
        private Long projectId;
        private String projectName;
        private String projectDescription;
        private Long inviterId;
        private String inviterName;
        private ProjectRole offeredRole;

        public static InvitationInfo from(ProjectInvite invite) {
            return InvitationInfo.builder()
                    .inviteId(invite.getInviteId())
                    .projectId(invite.getProject().getProjectId())
                    .projectName(invite.getProject().getName())
                    .projectDescription(invite.getProject().getDescription())
                    .inviterId(invite.getInviter().getUserId())
                    .inviterName(invite.getInviter().getName())
                    .offeredRole(ProjectRole.MEMBER)
                    .build();
        }
    }
}
