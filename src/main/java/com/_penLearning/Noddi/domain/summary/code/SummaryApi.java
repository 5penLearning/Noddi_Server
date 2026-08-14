package com._penLearning.Noddi.domain.summary.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.summary.dto.SummaryRequestDto;
import com._penLearning.Noddi.domain.summary.dto.SummaryResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface SummaryApi {

    @Operation(
            summary = "AI 회의록 상세 조회",
            description = "회의의 AI 처리 상태와 요약, 결정사항, 논의 이슈, 원문 및 ActionItem을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "AI 회의록 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 회의 팀원이 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회의를 찾을 수 없음"
            )
    })
    ApiResponse<SummaryResponseDto.Detail> getSummary(
            Long meetingId,
            @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(
            summary = "AI 회의록 수정",
            description = "AI가 생성한 전체 요약, 결정사항 및 논의 이슈를 수정합니다."
    )
    ApiResponse<Void> updateSummary(
            Long meetingId,
            SummaryRequestDto.Update request,
            @AuthenticationPrincipal AuthMember authMember
    );
}
