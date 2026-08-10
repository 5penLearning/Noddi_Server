package com._penLearning.Noddi.domain.team.dto;

import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamInvite;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

public class TeamResponseDto {

    @Getter
    @Builder
    public static class TeamInfo {
        private Long teamId;
        private String name;
        private String description;
        private TeamRole myRole;

        public static TeamInfo from(TeamMember teamMember) {
            return TeamInfo.builder()
                    .teamId(teamMember.getTeam().getTeamId())
                    .name(teamMember.getTeam().getName())
                    .description(teamMember.getTeam().getDescription())
                    .myRole(teamMember.getRole())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ProjectTeamInfo {
        private Long teamId;
        private String name;
        private String description;
        private String creatorName;

        public static ProjectTeamInfo from(Team team) {
            return ProjectTeamInfo.builder()
                    .teamId(team.getTeamId())
                    .name(team.getName())
                    .description(team.getDescription())
                    .creatorName(team.getCreatedBy().getName())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MemberInfo {
        private Long userId;
        private String name;
        private String email;
        private TeamRole role;

        public static MemberInfo from(TeamMember teamMember) {
            return MemberInfo.builder()
                    .userId(teamMember.getUser().getUserId()) // User 엔티티의 ID 필드명에 맞게 수정
                    .name(teamMember.getUser().getName())
                    .email(teamMember.getUser().getEmail())
                    .role(teamMember.getRole())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class InvitationInfo {
        private Long inviteId;
        private Long teamId;
        private String teamName;
        private String inviterName;

        public static InvitationInfo from(TeamInvite invite) {
            return InvitationInfo.builder()
                    .inviteId(invite.getInviteId())
                    .teamId(invite.getTeam().getTeamId())
                    .teamName(invite.getTeam().getName())
                    .inviterName(invite.getInviter().getName())
                    .build();
        }
    }
}
