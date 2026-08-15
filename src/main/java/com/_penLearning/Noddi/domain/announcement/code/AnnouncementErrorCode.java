package com._penLearning.Noddi.domain.announcement.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum AnnouncementErrorCode implements BaseErrorCode {

    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANNOUNCEMENT404_1", "해당 공지를 찾을 수 없습니다."),
    YOU_ARE_NOT_AUTHOR(HttpStatus.FORBIDDEN, "ANNOUNCEMENT403_1", "작성자만 수정, 삭제할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
