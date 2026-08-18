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
    INVALID_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "USER400_3", "JPEG, PNG, WebP 형식의 프로필 이미지만 업로드할 수 있습니다."),
    PROFILE_IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "USER413_1", "프로필 이미지는 최대 5MB까지 업로드할 수 있습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자를 찾을 수 없습니다."),
    PROFILE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_2", "프로필 이미지를 찾을 수 없습니다."),

    PROFILE_IMAGE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER500_1", "프로필 이미지 저장 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
