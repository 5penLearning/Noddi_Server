package com._penLearning.Noddi.domain.meeting.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.meeting.code.MeetingApi;
import com._penLearning.Noddi.domain.meeting.dto.MeetingRequestDto;
import com._penLearning.Noddi.domain.meeting.dto.MeetingResponseDto;
import com._penLearning.Noddi.domain.meeting.service.MeetingService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meeting")
@RequiredArgsConstructor
public class MeetingController implements MeetingApi {

    private final MeetingService meetingService;

    @Override
    @PostMapping
    public ApiResponse<MeetingResponseDto.Info> createMeeting(
            @RequestBody@Valid MeetingRequestDto.Create request, @AuthenticationPrincipal AuthMember authMember) {
        MeetingResponseDto.Info response = meetingService.createMeeting(request, authMember.getUserId());
        return ApiResponse.onSuccess("회의가 예약되었습니다.", response);
    }

    @Override
    @PatchMapping("/{meetingId}/start")
    public ApiResponse<MeetingResponseDto.Start> startMeeting(
            @PathVariable Long meetingId, @AuthenticationPrincipal AuthMember authMember) {
        MeetingResponseDto.Start response = meetingService.startMeeting(meetingId, authMember.getUserId());
        return ApiResponse.onSuccess("회의가 시작되었습니다.", response);
    }

    @Override
    @PatchMapping("/{meetingId}/end")
    public ApiResponse<Void> endMeeting(
            @PathVariable Long meetingId, @AuthenticationPrincipal AuthMember authMember) {
        meetingService.endMeeting(meetingId, authMember.getUserId());
        return ApiResponse.onSuccess("회의가 종료되었습니다.");
    }

    @Override
    @PostMapping("/{meetingId}/summary")
    public ApiResponse<Void> triggerSummary(
            @PathVariable Long meetingId, @AuthenticationPrincipal AuthMember authMember) {
        meetingService.triggerSummary(meetingId, authMember.getUserId());
        return ApiResponse.onSuccess("AI 요약 요청이 접수되었습니다. 잠시 후 결과를 확인해 주세요.");
    }
}
