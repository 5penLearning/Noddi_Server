package com._penLearning.Noddi.global.security;

import com._penLearning.Noddi.domain.auth.code.AuthErrorCode;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        // 403 Forbidden 에러 코드 지정 (AuthErrorCode에 FORBIDDEN 또는 ACCESS_DENIED 항목 정의 필요)
        AuthErrorCode errorCode = AuthErrorCode.FORBIDDEN;

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        ApiResponse<Void> apiResponse = ApiResponse.onFailure(errorCode, null);

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
