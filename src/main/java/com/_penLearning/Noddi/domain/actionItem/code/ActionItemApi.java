package com._penLearning.Noddi.domain.actionItem.code;

import com._penLearning.Noddi.domain.actionItem.dto.ActionItemRequestDto;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(
        name = "ActionItem API",
        description = "회의 이후 수행할 ActionItem 관리 API"
)
public interface ActionItemApi {

    @Operation(
            summary = "ActionItem 생성",
            description = "회의에 새로운 ActionItem을 추가합니다."
    )
    ApiResponse<Long> createActionItem(
            Long meetingId,
            ActionItemRequestDto.Create request,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "내 ActionItem 목록 조회",
            description = "현재 로그인한 사용자가 담당자로 지정된 미완료 ActionItem(PENDING, IN_PROGRESS)을 조회합니다."
    )
    ApiResponse<List<ActionItemResponseDto.Info>> getMyActionItems(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "팀별 개인 To-do 조회",
            description = "현재 사용자가 소속된 모든 팀과 담당 미완료 ActionItem(PENDING, IN_PROGRESS)을 팀별로 묶어 조회합니다. 할 일이 없는 팀도 포함합니다."
    )
    ApiResponse<List<ActionItemResponseDto.TeamTodoGroup>>
    getMyActionItemsByTeam(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "ActionItem 수정",
            description = "ActionItem의 내용, 담당자, 기한 및 진행 상태를 수정합니다."
    )
    ApiResponse<Void> updateActionItem(
            Long actionItemId,
            ActionItemRequestDto.Update request,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "ActionItem 삭제",
            description = "회의에 등록된 ActionItem을 삭제합니다."
    )
    ApiResponse<Void> deleteActionItem(
            Long actionItemId,
            @Parameter(hidden = true) AuthMember authMember
    );
}
