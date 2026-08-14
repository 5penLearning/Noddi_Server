package com._penLearning.Noddi.domain.actionItem.controller;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemApi;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemRequestDto;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.actionItem.service.ActionItemCommandService;
import com._penLearning.Noddi.domain.actionItem.service.ActionItemQueryService;
import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActionItemController implements ActionItemApi {

    private final ActionItemCommandService actionItemCommandService;
    private final ActionItemQueryService actionItemQueryService;

    @Override
    @PostMapping("/meetings/{meetingId}/action-items")
    public ApiResponse<Long> createActionItem(
            @PathVariable Long meetingId,
            @Valid @RequestBody ActionItemRequestDto.Create request,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long actionItemId = actionItemCommandService.createActionItem(
                meetingId,
                authMember.getUserId(),
                request
        );

        return ApiResponse.onSuccess(
                "ActionItem이 생성되었습니다.",
                actionItemId
        );
    }

    @Override
    @GetMapping("/action-items/me")
    public ApiResponse<List<ActionItemResponseDto.Info>> getMyActionItems(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<ActionItemResponseDto.Info> response =
                actionItemQueryService.getMyActionItems(
                        authMember.getUserId()
                );

        return ApiResponse.onSuccess(
                "내 ActionItem 목록 조회가 완료되었습니다.",
                response
        );
    }

    @Override
    @PatchMapping("/action-items/{actionItemId}")
    public ApiResponse<Void> updateActionItem(
            @PathVariable Long actionItemId,
            @Valid @RequestBody ActionItemRequestDto.Update request,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        actionItemCommandService.updateActionItem(
                actionItemId,
                authMember.getUserId(),
                request
        );

        return ApiResponse.onSuccess(
                "ActionItem이 수정되었습니다."
        );
    }

    @Override
    @DeleteMapping("/action-items/{actionItemId}")
    public ApiResponse<Void> deleteActionItem(
            @PathVariable Long actionItemId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        actionItemCommandService.deleteActionItem(
                actionItemId,
                authMember.getUserId()
        );

        return ApiResponse.onSuccess(
                "ActionItem이 삭제되었습니다."
        );
    }
}