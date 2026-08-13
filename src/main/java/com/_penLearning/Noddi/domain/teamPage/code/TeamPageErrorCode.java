package com._penLearning.Noddi.domain.teamPage.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamPageErrorCode implements BaseErrorCode {

    YOU_ARE_NOT_AUTHOR(HttpStatus.FORBIDDEN, "TEAM_PAGE403_1", "작성자만 글을 수정ㆍ삭제할 수 있습니다."),
    TEAM_PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_PAGE404_1", "페이지를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
