package com._penLearning.Noddi.domain.qa.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QaErrorCode implements BaseErrorCode {

    INVALID_AI_ANSWER_CITATION(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "QA500_2",
            "AI 답변에서 유효한 근거를 확인할 수 없습니다."
    ),

    AI_ANSWER_GENERATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "QA500_1",
            "AI 답변을 생성하지 못했습니다."
    ),

    INVALID_QUESTION_STATUS(HttpStatus.CONFLICT, "QA409_2", "현재 질문 상태에서는 요청한 작업을 수행할 수 없습니다."),

    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QA404_1", "질문을 찾을 수 없습니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "QA404_2", "답변을 찾을 수 없습니다."),

    NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "QA403_1", "해당 프로젝트의 멤버가 아닙니다."),
    NOT_TARGET_TEAM_MEMBER(HttpStatus.FORBIDDEN, "QA403_2", "질문 대상 팀의 멤버만 답변할 수 있습니다."),

    ALREADY_ANSWERED(HttpStatus.CONFLICT, "QA409_1", "이미 답변이 완료된 질문입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
