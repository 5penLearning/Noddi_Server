package com._penLearning.Noddi.domain.notification.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.notification.code.NotificationApi;
import com._penLearning.Noddi.domain.notification.dto.NotificationFilter;
import com._penLearning.Noddi.domain.notification.dto.NotificationRequestDto;
import com._penLearning.Noddi.domain.notification.dto.NotificationResponseDto;
import com._penLearning.Noddi.domain.notification.service.NotificationCommandService;
import com._penLearning.Noddi.domain.notification.service.NotificationQueryService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    /**
     * 알림함 목록 조회
     *
     * filter를 생략하면 ALL이 적용된다.
     * 따라서 프론트는 최초 알림함 진입 시 별도의 필터를 보내지 않아도 된다.
     */
    @Override
    @GetMapping
    public ApiResponse<NotificationResponseDto.NotificationList>
    getNotifications(
            @AuthenticationPrincipal AuthMember authMember,

            @RequestParam(defaultValue = "ALL")
            NotificationFilter filter,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        NotificationResponseDto.NotificationList response =
                notificationQueryService.getNotifications(
                        authMember.getUserId(),
                        filter,
                        page,
                        size
                );

        return ApiResponse.onSuccess(
                "알림 목록 조회에 성공했습니다.",
                response
        );
    }

    /**
     * 개별 알림 자세히보기
     *
     * 읽음 처리 API가 성공한 뒤 프론트는
     * 조회 응답에 포함된 navigation 정보로 화면을 이동한다.
     */
    @Override
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        notificationCommandService.markAsRead(
                authMember.getUserId(),
                notificationId
        );

        return ApiResponse.onSuccess(
                "알림을 읽음 처리했습니다."
        );
    }

    /**
     * AI 답변 검토 묶음 자세히보기
     *
     * 같은 사용자·프로젝트·팀에 속한 안 읽은 검토 알림을
     * 모두 읽음 처리한다.
     */
    @Override
    @PatchMapping("/groups/read")
    public ApiResponse<Void> readNotificationGroup(
            @RequestBody @Valid NotificationRequestDto.ReadGroup request,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        notificationCommandService.markGroupAsRead(
                authMember.getUserId(),
                request.getProjectId(),
                request.getTeamId(),
                request.getType()
        );

        return ApiResponse.onSuccess(
                "묶음 알림을 읽음 처리했습니다."
        );
    }

    /**
     * 개별 알림 X 버튼
     *
     * 서비스의 hide()가 읽음과 숨김을 함께 처리한다.
     */
    @Override
    @PatchMapping("/{notificationId}/hide")
    public ApiResponse<Void> hideNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        notificationCommandService.hide(
                authMember.getUserId(),
                notificationId
        );

        return ApiResponse.onSuccess(
                "알림을 숨김 처리했습니다."
        );
    }

    /**
     * 묶음 알림 X 버튼
     *
     * read를 함께 전달해 ALL 화면에 나타난
     * 읽은 묶음 또는 안 읽은 묶음 중 하나만 숨긴다.
     */
    @Override
    @PatchMapping("/groups/hide")
    public ApiResponse<Void> hideNotificationGroup(
            @RequestBody @Valid NotificationRequestDto.HideGroup request,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        notificationCommandService.hideGroup(
                authMember.getUserId(),
                request.getProjectId(),
                request.getTeamId(),
                request.getType(),
                request.getRead()
        );

        return ApiResponse.onSuccess(
                "묶음 알림을 숨김 처리했습니다."
        );
    }
}