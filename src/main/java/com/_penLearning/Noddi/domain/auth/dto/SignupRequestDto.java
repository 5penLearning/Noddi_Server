package com._penLearning.Noddi.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequestDto(
        @Schema(description = "조직 아이디", example = "1")
        @NotNull(message = "조직은 필수 선택 사항입니다.")
        Long organizationId,
        @Schema(description = "이메일", example = "example@gmail.com")
        @NotBlank(message = "이메일은 필수 형식입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "비밀번호", example = "test1234")
        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,

        @Schema(description = "이름", example = "김철수")
        @NotBlank(message = "이름은 필수 입력값입니다.")
        String name
) {
}
