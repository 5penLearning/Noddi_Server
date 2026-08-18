package com._penLearning.Noddi.domain.home.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(
        name = "홈 AI 답변 현황 API",
        description = "현재 사용자가 검토해야 할 AI 답변 현황 조회 API"
)
public interface HomeAiAnswerApi {

    @Operation(
            summary = "프로젝트별 미확인 AI 답변 개수 조회",
            description = "현재 사용자가 소속된 팀의 미확인 AI 답변을 프로젝트별로 집계합니다."
    )
    ApiResponse<List<HomeAiAnswerResponseDto.ProjectStatus>>
    getUnreadAnswerCountsByProject(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "프로젝트의 미확인 AI 답변 카드 조회",
            description = "선택한 프로젝트에서 현재 사용자가 검토해야 할 AI 답변을 최신순으로 조회합니다."
    )
    ApiResponse<HomeAiAnswerResponseDto.CardPage> getUnreadAnswerCards(
            @Parameter(description = "프로젝트 ID") Long projectId,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0") int page,
            @Parameter(description = "페이지 크기, 1 이상 50 이하", example = "10") int size,
            @Parameter(hidden = true) AuthMember authMember
    );
}
