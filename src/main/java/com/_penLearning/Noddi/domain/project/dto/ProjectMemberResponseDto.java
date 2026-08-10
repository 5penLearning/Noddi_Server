package com._penLearning.Noddi.domain.project.dto;

import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import lombok.Builder;
import lombok.Getter;

public class ProjectMemberResponseDto {

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
        private Long projectId;
        private String projectName;
        private String projectDescription;
        private ProjectRole offeredRole; // 어떤 권한으로 초대받았는지 명시

        public static InvitationInfo from(ProjectMember projectMember) {
            return InvitationInfo.builder()
                    .projectId(projectMember.getProject().getProjectId())
                    .projectName(projectMember.getProject().getName()) // N:1 연관관계(Fetch Join)로 가져온 프로젝트 이름
                    .projectDescription(projectMember.getProject().getDescription())
                    .offeredRole(projectMember.getRole())
                    .build();
        }
    }
}
