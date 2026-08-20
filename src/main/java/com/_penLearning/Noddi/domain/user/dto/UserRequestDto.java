package com._penLearning.Noddi.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDto {

    @Getter
    @NoArgsConstructor
    public static class UpdateProfile {
        @NotBlank(message = "변경할 이름을 입력해주세요.")
        private String name;

        @Size(max = 20, message = "부서는 최대 20자까지 입력 가능합니다.")
        @Pattern(regexp = ".*\\S.*", message = "부서는 공백만 입력할 수 없습니다.")
        private String department;

        @Size(max = 20, message = "직함은 최대 20자까지 입력 가능합니다.")
        @Pattern(regexp = ".*\\S.*", message = "직함은 공백만 입력할 수 없습니다.")
        private String position;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdatePassword {

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;

        @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다."
        )        private String newPassword;
    }
}
