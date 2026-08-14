package com._penLearning.Noddi.domain.summary.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.summary.code.SummaryApi;
import com._penLearning.Noddi.domain.summary.dto.SummaryRequestDto;
import com._penLearning.Noddi.domain.summary.dto.SummaryResponseDto;
import com._penLearning.Noddi.domain.summary.service.SummaryCommandService;
import com._penLearning.Noddi.domain.summary.service.SummaryQueryService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class SummaryController implements SummaryApi {

    private final SummaryQueryService summaryQueryService;
    private final SummaryCommandService summaryCommandService;

    @Override
    @GetMapping("/{meetingId}/summary")
    public ApiResponse<SummaryResponseDto.Detail> getSummary(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        SummaryResponseDto.Detail response =
                summaryQueryService.getSummary(
                        meetingId,
                        authMember.getUserId()
                );

        return ApiResponse.onSuccess(
                "AI 회의록 상세 조회가 완료되었습니다.",
                response
        );
    }

    @Override
    @PatchMapping("/{meetingId}/summary")
    public ApiResponse<Void> updateSummary(
            @PathVariable Long meetingId,
            @Valid @RequestBody SummaryRequestDto.Update request,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        summaryCommandService.updateSummary(
                meetingId,
                authMember.getUserId(),
                request
        );

        return ApiResponse.onSuccess(
                "AI 회의록이 수정되었습니다."
        );
    }
}