package com._penLearning.Noddi.domain.auth.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH400_1", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INACTIVE_USER(HttpStatus.FORBIDDEN, "AUTH400_2", "비활성화된 계정입니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "AUTH400_3", "이미 사용 중인 이메일입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH400_4", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH400_5", "만료된 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
