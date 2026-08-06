package com._penLearning.Noddi.domain.auth.controller;

import com._penLearning.Noddi.domain.auth.code.AuthApi;
import com._penLearning.Noddi.domain.auth.dto.LoginRequestDto;
import com._penLearning.Noddi.domain.auth.dto.SignupRequestDto;
import com._penLearning.Noddi.domain.auth.dto.TokenResponseDto;
import com._penLearning.Noddi.domain.auth.service.AuthService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<ApiResponse<Void>> signup(SignupRequestDto request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess("회원가입이 완료되었습니다."));
    }

    @Override
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(LoginRequestDto request) {
        TokenResponseDto tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.onSuccess("로그인 성공", tokenResponse));
    }
}
