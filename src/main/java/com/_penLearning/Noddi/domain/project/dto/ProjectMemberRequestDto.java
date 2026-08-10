package com._penLearning.Noddi.domain.project.dto;

import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ProjectMemberRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Invite {
        @NotNull(message = "초대할 유저의 ID는 필수입니다.")
        private Long targetUserId;
        // 유저 ID 대신 이메일(String targetEmail)로 초대하게 될 경우 이 필드를 변경해야 함
    }

    @Getter
    @NoArgsConstructor
    public static class Respond {
        @NotNull(message = "수락 여부는 필수 입력값입니다.")
        private Boolean isAccepted;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRole {
        @NotNull(message = "변경할 권한은 필수 입력값입니다.")
        private ProjectRole newRole;
    }
}
