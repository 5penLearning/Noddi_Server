package com._penLearning.Noddi.domain.project.code;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {

    // 400 BAD_REQUEST
    INVALID_PROJECT_REQUEST(HttpStatus.BAD_REQUEST, "PROJECT400_1", "잘못된 프로젝트 요청입니다."),
    NOT_SAME_ORGANIZATION(HttpStatus.BAD_REQUEST, "PROJECT400_2", "다른 조직 멤버에게는 초대를 전송할 수 없습니다."),
    CANNOT_REMOVE_LAST_LEADER(HttpStatus.BAD_REQUEST, "PROJECT400_3", "프로젝트의 마지막 리더는 탈퇴하거나 권한을 강등할 수 없습니다."),
    INVITE_NOT_PENDING(HttpStatus.BAD_REQUEST, "PROJECT400_4", "이미 처리되었거나 만료된 초대장입니다."),
    INVITE_EXPIRED(HttpStatus.BAD_REQUEST, "PROJECT400_5", "만료된 초대장입니다."),

    // 403 FORBIDDEN (권한 없음)
    NOT_PROJECT_LEADER(HttpStatus.FORBIDDEN, "PROJECT403_1", "프로젝트 관리자(LEADER) 권한이 필요합니다."),
    NOT_INVITEE(HttpStatus.FORBIDDEN, "PROJECT403_2", "해당 초대의 대상자가 아닙니다."),

    // 404 NOT_FOUND (존재하지 않음)
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404_1", "해당 프로젝트를 찾을 수 없습니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404_2", "해당 프로젝트의 멤버가 아닙니다."),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404_3", "초대장을 찾을 수 없습니다."),

    // 409 CONFLICT (충돌/중복)
    ALREADY_PROJECT_MEMBER(HttpStatus.CONFLICT, "PROJECT409_1", "이미 프로젝트에 참여 중인 유저입니다."),
    ALREADY_INVITED_USER(HttpStatus.CONFLICT, "PROJECT409_2", "이미 초대를 받고 대기 중인 유저입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
