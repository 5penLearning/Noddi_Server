package com._penLearning.Noddi.domain.teamPage.controller;


import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.teamPage.code.TeamPageApi;
import com._penLearning.Noddi.domain.teamPage.dto.TeamPageRequestDto;
import com._penLearning.Noddi.domain.teamPage.dto.TeamPageResponseDto;
import com._penLearning.Noddi.domain.teamPage.service.TeamPageService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/pages")
@RequiredArgsConstructor
public class TeamPageController implements TeamPageApi {

    private final TeamPageService teamPageService;

    @PostMapping
    @Override
    public ApiResponse<TeamPageResponseDto.Result> createPage(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamPageRequestDto.Create request
    ) {
        TeamPageResponseDto.Result response = teamPageService.createPage(
                teamId,
                authMember.getUserId(),
                request
        );

        return ApiResponse.onSuccess("팀 페이지가 생성되었습니다.", response);
    }

    @PatchMapping("/{pageId}")
    @Override
    public ApiResponse<TeamPageResponseDto.Result> updatePage(
            @PathVariable Long teamId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamPageRequestDto.Update request
    ){
        TeamPageResponseDto.Result response = teamPageService.updatePage(
                teamId,
                pageId,
                authMember.getUserId(),
                request
        );
        return ApiResponse.onSuccess("팀 페이지가 수정되었습니다.", response);
    }

    @GetMapping("/{pageId}")
    @Override
    public ApiResponse<TeamPageResponseDto.Detail> getTeamPage(
            @PathVariable Long teamId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal AuthMember authMember
    ){
        TeamPageResponseDto.Detail response = teamPageService.getTeamPage(teamId, pageId, authMember.getUserId());
        return ApiResponse.onSuccess("팀 페이지 상세 조회에 성공했습니다.", response);
    }

    @GetMapping
    @Override
    public ApiResponse<Page<TeamPageResponseDto.Summary>> getTeamPages(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            @PageableDefault(
                    size = 10,
                    sort = {"updatedAt", "pageId"},
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ){
        Page<TeamPageResponseDto.Summary> response =
                teamPageService.getTeamPages(
                        teamId,
                        authMember.getUserId(),
                        pageable
                );
        return ApiResponse.onSuccess("팀 페이지 목록 조회에 성공했습니다.", response);
    }

    @DeleteMapping("/{pageId}")
    @Override
    public ApiResponse<Void> deleteTeamPage(
            @PathVariable Long teamId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal AuthMember authMember
    ){
        teamPageService.deleteTeamPage(teamId, pageId, authMember.getUserId());
        return ApiResponse.onSuccess("팀 페이지 삭제에 성공했습니다.", null);
    }
}
