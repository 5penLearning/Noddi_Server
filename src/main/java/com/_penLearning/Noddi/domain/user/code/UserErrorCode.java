package com._penLearning.Noddi.domain.user.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {


    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "USER400_1", "현재 비밀번호가 일치하지 않습니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "USER400_2", "새로운 비밀번호는 기존 비밀번호와 달라야 합니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자를 찾을 수 없습니다." );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
