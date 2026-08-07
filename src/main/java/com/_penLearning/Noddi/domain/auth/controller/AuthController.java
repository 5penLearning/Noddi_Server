package com._penLearning.Noddi.domain.auth.controller;

import com._penLearning.Noddi.domain.auth.code.AuthApi;
import com._penLearning.Noddi.domain.auth.dto.AuthRequestDto;
import com._penLearning.Noddi.domain.auth.dto.AuthResponseDto;
import com._penLearning.Noddi.domain.auth.service.AuthService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    @PostMapping("/signup")
    public ApiResponse<AuthResponseDto.AuthSignupResponseDto> signup(
            @Valid @RequestBody AuthRequestDto.SignupRequestDto request) {

        AuthResponseDto.AuthSignupResponseDto response = authService.signup(request);

        return ApiResponse.onSuccess("회원가입이 완료되었습니다.", response);
    }

    @Override
    @PostMapping("/login")
    public ApiResponse<AuthResponseDto.AuthLoginResponseDto> login(
            @Valid @RequestBody AuthRequestDto.LoginRequestDto request) {

        AuthResponseDto.AuthLoginResponseDto response = authService.login(request);

        return ApiResponse.onSuccess("로그인 성공", response);
    }
}
