package com._penLearning.Noddi.domain.home.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HomeErrorCode implements BaseErrorCode {

    INVALID_PAGE_REQUEST(
            HttpStatus.BAD_REQUEST,
            "HOME400_1",
            "페이지는 0 이상, 조회 크기는 1 이상 50 이하여야 합니다."
    ),

    NOT_PROJECT_MEMBER(
            HttpStatus.FORBIDDEN,
            "HOME403_1",
            "해당 프로젝트의 멤버가 아닙니다."
    ),

    AI_ANSWER_DATA_NOT_FOUND(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "HOME500_1",
            "홈 AI 답변 카드 데이터를 구성할 수 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
