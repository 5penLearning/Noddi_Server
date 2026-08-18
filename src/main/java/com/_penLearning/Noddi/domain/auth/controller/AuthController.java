package com._penLearning.Noddi.domain.auth.controller;

import com._penLearning.Noddi.domain.auth.code.AuthApi;
import com._penLearning.Noddi.domain.auth.dto.AuthRequestDto;
import com._penLearning.Noddi.domain.auth.dto.AuthResponseDto;
import com._penLearning.Noddi.domain.auth.service.AuthService;
import com._penLearning.Noddi.domain.auth.service.EmailService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final EmailService emailService;

    @Override
    @PostMapping("/signup")
    public ApiResponse<AuthResponseDto.AuthSignupResponseDto> signup(
            @Valid @RequestBody AuthRequestDto.SignupRequestDto request) {

        AuthResponseDto.AuthSignupResponseDto response = authService.signup(request);

        return ApiResponse.onSuccess("회원가입이 완료되었습니다.", response);
    }

    @Override
    @GetMapping("/profile-options")
    public ApiResponse<AuthResponseDto.SignupProfileOptions> getSignupProfileOptions(
            @RequestParam Long organizationId
    ) {
        return ApiResponse.onSuccess(
                "부서 및 직함 추천 목록 조회에 성공했습니다.",
                authService.getSignupProfileOptions(organizationId)
        );
    }

    @Override
    @PostMapping("/login")
    public ApiResponse<AuthResponseDto.AuthLoginResponseDto> login(
            @Valid @RequestBody AuthRequestDto.LoginRequestDto request) {

        AuthResponseDto.AuthLoginResponseDto response = authService.login(request);

        return ApiResponse.onSuccess("로그인 성공", response);
    }

    @Override
    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody AuthRequestDto.EmailSendRequestDto request) {
        emailService.sendVerificationCode(request);
        return ApiResponse.onSuccess("인증번호가 발송되었습니다.", null);
    }

    @Override
    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(@Valid @RequestBody AuthRequestDto.EmailVerifyRequestDto request) {
        emailService.verifyCode(request);
        return ApiResponse.onSuccess("이메일 인증이 완료되었습니다.", null);
    }
}
