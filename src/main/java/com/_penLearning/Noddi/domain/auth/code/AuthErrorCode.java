package com._penLearning.Noddi.domain.auth.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 400 BAD_REQUEST
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "AUTH400_1", "올바르지 않은 입력값입니다."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "AUTH400_2", "해당 조직의 이메일 도메인과 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH400_3", "인증번호가 일치하지 않거나 만료되었습니다."),
    UNVERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "AUTH400_4", "이메일 인증이 완료되지 않았습니다."),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH400_5", "인증번호가 만료되었습니다."),
    EXCEEDED_VERIFICATION_ATTEMPTS(HttpStatus.BAD_REQUEST, "AUTH400_6", "인증 시도 횟수(5회)를 초과했습니다. 인증번호를 다시 발송해 주세요."),

    // 401 UNAUTHORIZED
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH401_1", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_2", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_3", "만료된 토큰입니다."),

    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH403_1", "접근 권한이 없습니다."),

    // 409 CONFLICT
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH409_1", "이미 사용 중인 이메일입니다."),

    // 429 TOO_MANY_REQUESTS
    EMAIL_SEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "AUTH429_1", "이메일 발송은 1분에 한 번만 가능합니다."),

    // 500 INTERNAL_SERVER_ERROR
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_1", "이메일 발송 중 서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
