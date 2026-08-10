package com._penLearning.Noddi.domain.team.dto;

import com._penLearning.Noddi.domain.team.entity.TeamRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TeamRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotBlank(message = "팀 이름은 필수 입력값입니다.")
        private String name;

        private String description;
    }

    @Getter
    @NoArgsConstructor
    public static class InviteMember {
        @NotNull(message = "초대할 유저의 ID는 필수입니다.")
        private Long targetUserId;
    }

    @Getter
    @NoArgsConstructor
    public static class RespondInvite {
        @NotNull(message = "수락 여부는 필수 입력값입니다.")
        private Boolean isAccepted;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateInfo {
        @NotBlank(message = "팀 이름은 필수 입력값입니다.")
        private String name;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRole {
        @NotNull(message = "변경할 권한은 필수 입력값입니다.")
        private TeamRole role;
    }
}
