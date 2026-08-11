package com._penLearning.Noddi.domain.qa.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QaErrorCode implements BaseErrorCode {

    INVALID_ANSWER_TYPE(HttpStatus.BAD_REQUEST, "QA400_1", "답변 유형이 올바르지 않습니다."),
    INVALID_ANSWERER(HttpStatus.BAD_REQUEST, "QA400_2", "답변 유형과 답변자 정보가 일치하지 않습니다."),

    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QA404_1", "질문을 찾을 수 없습니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "QA404_2", "답변을 찾을 수 없습니다."),

    NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "QA403_1", "해당 프로젝트의 멤버가 아닙니다."),
    NOT_TARGET_TEAM_MEMBER(HttpStatus.FORBIDDEN, "QA403_2", "질문 대상 팀의 멤버만 답변할 수 있습니다."),

    ALREADY_ANSWERED(HttpStatus.CONFLICT, "QA409_1", "이미 답변이 완료된 질문입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
