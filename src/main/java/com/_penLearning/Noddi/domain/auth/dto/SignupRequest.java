package com._penLearning.Noddi.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "조직은 필수 선택 사항입니다.")
        Long organizationId,

        @NotBlank(message = "이메일은 필수 형식입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        String name
) {
}
