package com._penLearning.Noddi.domain.actionItem.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ActionItemErrorCode implements BaseErrorCode {
    ACTION_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTION_ITEM404_1", "할 일을 찾을 수 없습니다."),

    ASSIGNEE_NOT_TEAM_MEMBER(HttpStatus.BAD_REQUEST, "ACTION_ITEM400_1", "담당자는 해당 회의의 팀원이어야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
