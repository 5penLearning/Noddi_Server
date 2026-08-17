package com._penLearning.Noddi.domain.notification.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.notification.dto.NotificationFilter;
import com._penLearning.Noddi.domain.notification.dto.NotificationRequestDto;
import com._penLearning.Noddi.domain.notification.dto.NotificationResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "알림 API",
        description = "서비스 공통 알림함 조회 및 읽음·숨김 처리 API"
)
public interface NotificationApi {

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    현재 사용자의 숨기지 않은 알림을 최신순으로 조회합니다.
                    AI 답변 검토 요청은 프로젝트·팀·읽음 상태를 기준으로 묶어서 반환합니다.

                    filter:
                    - ALL: 전체 알림
                    - UNREAD: 안 읽은 알림
                    """
    )
    ApiResponse<NotificationResponseDto.NotificationList> getNotifications(
            @Parameter(hidden = true)
            AuthMember authMember,

            @Parameter(description = "알림 조회 필터", example = "ALL")
            NotificationFilter filter,

            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            int page,

            @Parameter(description = "페이지 크기, 1 이상 50 이하", example = "20")
            int size
    );

    @Operation(
            summary = "개별 알림 읽음 처리",
            description = """
                    개별 알림의 자세히보기를 눌렀을 때 호출합니다.
                    현재 로그인한 사용자의 알림만 읽음 처리할 수 있습니다.
                    """
    )
    ApiResponse<Void> readNotification(
            @Parameter(description = "알림 ID")
            Long notificationId,

            @Parameter(hidden = true)
            AuthMember authMember
    );

    @Operation(
            summary = "묶음 알림 읽음 처리",
            description = """
                    AI 답변 검토 묶음의 자세히보기를 눌렀을 때 호출합니다.
                    type은 현재 QA_AI_REVIEW_REQUIRED만 지원합니다.
                    해당 프로젝트·팀에 속한 현재 사용자의 안 읽은 검토 알림을
                    모두 읽음 처리합니다.
                    """
    )
    ApiResponse<Void> readNotificationGroup(
            NotificationRequestDto.ReadGroup request,

            @Parameter(hidden = true)
            AuthMember authMember
    );

    @Operation(
            summary = "개별 알림 숨김 처리",
            description = """
                    개별 알림의 X 버튼을 눌렀을 때 호출합니다.
                    대상 알림은 읽음과 숨김 상태로 변경되며
                    이후 알림 목록에서 조회되지 않습니다.
                    """
    )
    ApiResponse<Void> hideNotification(
            @Parameter(description = "알림 ID")
            Long notificationId,

            @Parameter(hidden = true)
            AuthMember authMember
    );

    @Operation(
            summary = "묶음 알림 숨김 처리",
            description = """
                    묶음 알림의 X 버튼을 눌렀을 때 호출합니다.
                    type은 현재 QA_AI_REVIEW_REQUIRED만 지원합니다.
                    ALL 목록에서는 읽은 묶음과 안 읽은 묶음이 구분되므로
                    요청의 read 상태와 일치하는 묶음만 숨깁니다.
                    """
    )
    ApiResponse<Void> hideNotificationGroup(
            NotificationRequestDto.HideGroup request,

            @Parameter(hidden = true)
            AuthMember authMember
    );
}
