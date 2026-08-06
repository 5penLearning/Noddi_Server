package com._penLearning.Noddi.global.security;

import com._penLearning.Noddi.domain.auth.code.AuthErrorCode;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // 필터에서 저장했던 에러 코드 조회 (없으면 기본 토큰 에러)
        AuthErrorCode errorCode = (AuthErrorCode) request.getAttribute("exception");

        if (errorCode == null) {
            errorCode = AuthErrorCode.INVALID_TOKEN;
        }

        // 응답 헤더 및 Status 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        // 팀에서 사용중인 ApiResponse 통일 응답 생성
        ApiResponse<Void> apiResponse = ApiResponse.onFailure(errorCode, null);

        // JSON으로 직렬화하여 클라이언트에 출력
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
