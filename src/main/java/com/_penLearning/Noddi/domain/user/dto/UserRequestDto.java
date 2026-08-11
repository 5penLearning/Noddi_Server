package com._penLearning.Noddi.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDto {

    @Getter
    @NoArgsConstructor
    public static class UpdateProfile {
        @NotBlank(message = "변경할 이름을 입력해주세요.")
        private String name;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdatePassword {

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;

        @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
        // 정규식이 필요하다면 @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)...") 추가
        private String newPassword;
    }
}
