package com._penLearning.Noddi.domain.organization.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrganizationErrorCode implements BaseErrorCode {

    ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ORGANIZATION404_1", "해당 조직은 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
