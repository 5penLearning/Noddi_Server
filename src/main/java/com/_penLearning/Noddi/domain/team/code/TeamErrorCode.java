package com._penLearning.Noddi.domain.team.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements BaseErrorCode {

    // 400 BAD_REQUEST
    INVITE_NOT_PENDING(HttpStatus.BAD_REQUEST, "TEAM400_1", "이미 처리되었거나 만료된 초대장입니다."),
    CANNOT_REMOVE_LAST_LEADER(HttpStatus.BAD_REQUEST, "TEAM400_3", "팀의 마지막 리더는 탈퇴하거나 권한을 강등할 수 없습니다."),


    // 403 FORBIDDEN (인가/권한 예외)
    NOT_TEAM_LEADER(HttpStatus.FORBIDDEN, "TEAM403_1", "팀의 리더만 수행할 수 있는 권한입니다."),
    NOT_PROJECT_MEMBER(HttpStatus.FORBIDDEN, "TEAM403_2", "프로젝트에 정식 가입된 상태여야 팀에 참여하거나 초대할 수 있습니다."),
    NOT_INVITEE(HttpStatus.FORBIDDEN, "TEAM403_3", "본인에게 온 초대장만 응답할 수 있습니다."),

    // 404 NOT_FOUND (조회 실패)
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM404_1", "팀을 찾을 수 없습니다."),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM404_2", "초대장을 찾을 수 없습니다."),

    // 409 CONFLICT (중복 방지)
    ALREADY_TEAM_MEMBER(HttpStatus.CONFLICT, "TEAM409_1", "이미 해당 팀에 가입된 유저입니다."),
    ALREADY_INVITED(HttpStatus.CONFLICT, "TEAM409_2", "이미 해당 유저에게 발송된 대기 중인 초대장이 존재합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
