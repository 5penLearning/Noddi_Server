package com._penLearning.Noddi.domain.announcement.controller;

import com._penLearning.Noddi.domain.announcement.code.AnnouncementApi;
import com._penLearning.Noddi.domain.announcement.dto.AnnouncementRequestDto;
import com._penLearning.Noddi.domain.announcement.dto.AnnouncementResponseDto;
import com._penLearning.Noddi.domain.announcement.service.AnnouncementService;
import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
public class AnnouncementController implements AnnouncementApi {

    private final AnnouncementService announcementService;

    @Override
    @PostMapping("/teams/{teamId}/announcements")
    public ApiResponse<AnnouncementResponseDto.Result> createAnnouncement(
            @PathVariable Long projectId,
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid AnnouncementRequestDto.Create request
    ){
        AnnouncementResponseDto.Result response = announcementService.createAnnouncement(projectId, teamId, authMember.getUserId(), request);
        return ApiResponse.onSuccess("공지사항 등록에 성공하였습니다.", response);
    }

    @Override
    @PutMapping("/announcements/{announcementId}")
    public ApiResponse<AnnouncementResponseDto.Result> updateAnnouncement(
            @PathVariable Long projectId,
            @PathVariable Long announcementId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid AnnouncementRequestDto.Update request
    ){
        AnnouncementResponseDto.Result response = announcementService.updateAnnouncement(projectId, announcementId, authMember.getUserId(), request);
        return ApiResponse.onSuccess("공지사항 수정에 성공하였습니다.", response);
    }

    @Override
    @GetMapping("/announcements/{announcementId}")
    public ApiResponse<AnnouncementResponseDto.Detail> getAnnouncement(
            @PathVariable Long projectId,
            @PathVariable Long announcementId,
            @AuthenticationPrincipal AuthMember authMember
    ){
        AnnouncementResponseDto.Detail response = announcementService.getAnnouncement(projectId, announcementId, authMember.getUserId());
        return ApiResponse.onSuccess("공지사항 상세 조회에 성공하였습니다.", response);
    }

    @Override
    @GetMapping("/announcements")
    public ApiResponse<Page<AnnouncementResponseDto.Summary>> getAnnouncements(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember authMember,
            @PageableDefault(size = 10) Pageable pageable
    ){
        Page<AnnouncementResponseDto.Summary> response = announcementService.getAnnouncements(projectId, authMember.getUserId(), pageable);
        return ApiResponse.onSuccess("공지사항 목록 조회에 성공하였습니다.", response);
    }

    @Override
    @DeleteMapping("/announcements/{announcementId}")
    public ApiResponse<Void> deleteAnnouncement(
            @PathVariable Long projectId,
            @PathVariable Long announcementId,
            @AuthenticationPrincipal AuthMember authMember
    ){
        announcementService.deleteAnnouncement(projectId, announcementId, authMember.getUserId());

        return ApiResponse.onSuccess("공지사항 삭제에 성공하였습니다.", null);
    }
}
