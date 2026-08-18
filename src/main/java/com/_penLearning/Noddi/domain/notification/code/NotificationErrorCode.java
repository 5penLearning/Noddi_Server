package com._penLearning.Noddi.domain.notification.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    INVALID_NOTIFICATION_GROUP(
            HttpStatus.BAD_REQUEST,
            "NOTIFICATION400_1",
            "묶음 처리를 지원하지 않는 알림 유형입니다."
    ),

    NOTIFICATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NOTIFICATION404_1",
            "알림을 찾을 수 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}