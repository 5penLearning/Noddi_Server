package com._penLearning.Noddi.domain.auth.code;

import com._penLearning.Noddi.domain.auth.dto.AuthRequestDto;
import com._penLearning.Noddi.domain.auth.dto.AuthResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "01. Auth API", description = "인증/인가 관련 API (회원가입, 로그인 등)")
public interface AuthApi {

    @Operation(summary = "회원가입", description = "신규 사용자를 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이메일 중복 또는 유효성 검사 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<AuthResponseDto.AuthSignupResponseDto> signup( AuthRequestDto.SignupRequestDto requestDto
    );

    @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인하여 JWT Access Token을 발급받습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "비활성화된 계정",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<AuthResponseDto.AuthLoginResponseDto> login(AuthRequestDto.LoginRequestDto requestDto
    );

    @Operation(summary = "이메일 인증번호 발송 API", description = "조직 도메인 검증 후 인증번호를 발송합니다.")
    ApiResponse<Void> sendEmailCode(AuthRequestDto.EmailSendRequestDto request);

    @Operation(summary = "이메일 인증번호 검증 API", description = "발송된 인증번호가 일치하는지 확인합니다.")
    ApiResponse<Void> verifyEmailCode(AuthRequestDto.EmailVerifyRequestDto request);
}
