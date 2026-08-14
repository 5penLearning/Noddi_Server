package com._penLearning.Noddi.domain.announcement.code;

import com._penLearning.Noddi.domain.announcement.dto.AnnouncementRequestDto;
import com._penLearning.Noddi.domain.announcement.dto.AnnouncementResponseDto;
import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Announcement API", description = "프로젝트 전체 공지 등록·조회·수정·삭제 API")
public interface AnnouncementApi {

    @Operation(
            summary = "공지 등록",
            description = "프로젝트에 속한 팀 멤버가 해당 팀 명의의 공지를 등록합니다."
    )
    ApiResponse<AnnouncementResponseDto.Result> createAnnouncement(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "공지 작성 팀 ID") @PathVariable Long teamId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody AnnouncementRequestDto.Create request
    );

    @Operation(
            summary = "공지 수정",
            description = "공지 작성자가 공지의 제목과 본문을 수정합니다."
    )
    ApiResponse<AnnouncementResponseDto.Result> updateAnnouncement(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "공지 ID") @PathVariable Long announcementId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody AnnouncementRequestDto.Update request
    );

    @Operation(
            summary = "공지 상세 조회",
            description = "프로젝트 멤버가 공지의 상세 내용을 조회합니다."
    )
    ApiResponse<AnnouncementResponseDto.Detail> getAnnouncement(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "공지 ID") @PathVariable Long announcementId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(
            summary = "프로젝트 전체 공지 목록 조회",
            description = "프로젝트 멤버가 프로젝트 내 모든 팀의 공지를 페이징하여 조회합니다."
    )
    ApiResponse<Page<AnnouncementResponseDto.Summary>> getAnnouncements(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "공지 삭제",
            description = "공지 작성자가 공지를 삭제합니다."
    )
    ApiResponse<Void> deleteAnnouncement(
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "공지 ID") @PathVariable Long announcementId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );
}
