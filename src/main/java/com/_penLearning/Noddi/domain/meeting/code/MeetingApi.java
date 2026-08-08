package com._penLearning.Noddi.domain.meeting.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.meeting.dto.MeetingRequestDto;
import com._penLearning.Noddi.domain.meeting.dto.MeetingResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "02. Meeting API", description = "회의 예약/시작/종료/AI 요약 관련 API")
public interface MeetingApi {

    @Operation(summary = "회의 예약 생성", description = "팀의 회의를 예약 상태(SCHEDULED)로 등록합니다. Daily.co 방은 아직 생성되지 않습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 예약 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효성 검사 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 팀의 팀원이 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "팀 또는 유저를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<MeetingResponseDto.Info> createMeeting(
            MeetingRequestDto.Create request,
            @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "회의 시작", description = "Daily.co 방을 생성하고 회의를 시작합니다(IN_PROGRESS). 이미 진행 중이면 기존 방 URL을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 시작 성공 (roomUrl 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "예약 상태가 아닌 회의를 시작 시도",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 팀의 팀원이 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회의를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Daily.co WebRTC 방 생성 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<MeetingResponseDto.Start> startMeeting(
            Long meetingId,
            @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "회의 종료", description = "회의를 종료(ENDED)하고 Daily.co 방을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회의 종료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "진행 중이 아닌 회의를 종료 시도",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 팀의 팀원이 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회의를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<Void> endMeeting(
            Long meetingId,
            @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "AI 요약 트리거", description = "녹음본이 준비된 종료된 회의에 대해 AI 요약을 수동으로 요청합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI 요약 요청 접수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "녹음본 미준비 또는 잘못된 회의 상태",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "해당 팀의 팀원이 아님",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회의를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<Void> triggerSummary(
            Long meetingId,
            @AuthenticationPrincipal AuthMember authMember
    );
}