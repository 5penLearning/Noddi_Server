package com._penLearning.Noddi.domain.summary.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SummaryErrorCode implements BaseErrorCode {
    STT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SUMMARY500_1", "회의 전문 텍스트 생성 중 오류가 발생했습니다."),
    SUMMARY_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SUMMARY500_2", "전문 요약 생성 중 오류가 발생했습니다."),

    SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "SUMMARY404_1", "회의록을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
