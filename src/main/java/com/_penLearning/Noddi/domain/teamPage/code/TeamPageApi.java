package com._penLearning.Noddi.domain.teamPage.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.teamPage.dto.TeamPageRequestDto;
import com._penLearning.Noddi.domain.teamPage.dto.TeamPageResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Team Page API", description = "팀 공유 페이지 생성·조회·수정·삭제 API")
public interface TeamPageApi {

    @Operation(summary = "팀 페이지 생성", description = "팀 멤버가 Markdown 형식의 공유 페이지를 생성합니다.")
    ApiResponse<TeamPageResponseDto.Result> createPage(
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody TeamPageRequestDto.Create request
    );

    @Operation(summary = "팀 페이지 수정", description = "페이지 작성자가 제목과 본문을 수정합니다.")
    ApiResponse<TeamPageResponseDto.Result> updatePage(
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @Parameter(description = "페이지 ID") @PathVariable Long pageId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody TeamPageRequestDto.Update request
    );

    @Operation(summary = "팀 페이지 상세 조회", description = "팀 멤버가 공유 페이지의 상세 내용을 조회합니다.")
    ApiResponse<TeamPageResponseDto.Detail> getTeamPage(
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @Parameter(description = "페이지 ID") @PathVariable Long pageId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "팀 페이지 목록 조회", description = "팀 페이지 목록을 최근 수정 순으로 조회합니다.")
    ApiResponse<Page<TeamPageResponseDto.Summary>> getTeamPages(
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable
    );

    @Operation(summary = "팀 페이지 삭제", description = "페이지 작성자가 공유 페이지를 삭제합니다.")
    ApiResponse<Void> deleteTeamPage(
            @Parameter(description = "팀 ID") @PathVariable Long teamId,
            @Parameter(description = "페이지 ID") @PathVariable Long pageId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );
}
