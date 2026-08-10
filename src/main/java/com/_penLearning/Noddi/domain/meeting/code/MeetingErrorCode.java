package com._penLearning.Noddi.domain.meeting.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.h2.api.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingErrorCode implements BaseErrorCode {
    INVALID_STATUS_FOR_START(HttpStatus.BAD_REQUEST, "MEETING400_1", "예약(SCHEDULED) 상태인 회의만 시작할 수 있습니다."),
    INVALID_STATUS_FOR_END(HttpStatus.BAD_REQUEST, "MEETING400_2", "진행 중(IN_PROGRESS)인 회의만 종료할 수 있습니다."),
    INVALID_STATUS_FOR_SUMMARY(HttpStatus.BAD_REQUEST, "MEETING400_3", "종료된(ENDED) 회의만 AI 요약을 요청할 수 있습니다."),
    ALREADY_PROCESSING_SUMMARY(HttpStatus.BAD_REQUEST, "MEETING400_4", "이미 AI 요약이 진행 중이거나 완료된 회의입니다."),
    RECORDING_NOT_READY(HttpStatus.BAD_REQUEST, "MEETING400_5", "녹음본 파일이 아직 인코딩 중입니다. 잠시 후 다시 시도해 주세요."),
    // 403 Forbidden: 해당 팀원이 아닌 경우
    NOT_TEAM_MEMBER(HttpStatus.FORBIDDEN, "MEETING403_1", "해당 팀의 팀원만 회의에 접근할 수 있습니다."),
    NOT_MEETING_CREATOR(HttpStatus.FORBIDDEN, "MEETING403_2", "해당 팀의 회의의 생성자만 종료할 수 있습니다."),
    // 404 Not Found: 회의를 찾을 수 없는 경우
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING404_1", "존재하지 않는 회의입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING404_2", "존재하지 않는 유저입니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING404_3", "존재하지 않는 팀입니다."),
    // 500 Internal Server Error: 외부 연동 장애
    WEBRTC_ROOM_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEETING500_1", "WebRTC 방 생성 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
