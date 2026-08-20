package com._penLearning.Noddi.domain.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthRequestDto {
    /**
     * 회원가입 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class SignupRequestDto {

        @NotNull(message = "조직 Id는 필수 입력 값입니다.")
        private Long organizationId;

        @NotBlank(message = "이름은 필수 입력 값입니다.")
        private String name;

        @NotBlank(message = "부서는 필수 입력 값입니다.")
        @Size(max = 20, message = "부서는 최대 20자까지 입력 가능합니다.")
        private String department;

        @NotBlank(message = "직함은 필수 입력 값입니다.")
        @Size(max = 20, message = "직함은 최대 20자까지 입력 가능합니다.")
        private String position;

        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max =254, message = "이메일은 최대 254자까지 입력 가능합니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        private String password;
    }

    /**
     * 로그인 요청 DTO
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class LoginRequestDto {

        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        private String password;
    }

    // 이메일 인증번호 발송 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailSendRequestDto {
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max =254, message = "이메일은 최대 254자까지 입력 가능합니다.")
        private String email;

        @NotNull(message = "조직 ID는 필수 입력값입니다.")
        private Long organizationId;
    }

    // 이메일 인증번호 검증 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailVerifyRequestDto {
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max =254, message = "이메일은 최대 254자까지 입력 가능합니다.")
        private String email;

        @NotNull(message = "조직 ID는 필수 입력값입니다.")
        private Long organizationId;

        @NotBlank(message = "인증 코드는 필수 입력값입니다.")
        private String code;
    }
}
