package com._penLearning.Noddi.domain.project.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.project.dto.ProjectMemberRequestDto;
import com._penLearning.Noddi.domain.project.dto.ProjectMemberResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "프로젝트 멤버(Project Member) API", description = "프로젝트 멤버 초대, 수락, 권한 변경 및 조회 API")
public interface ProjectMemberApi {

    @Operation(summary = "프로젝트 멤버 초대", description = "프로젝트 리더가 특정 유저를 프로젝트에 초대합니다.")
    ApiResponse<Void> inviteUser(
            @PathVariable Long projectId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @RequestBody ProjectMemberRequestDto.Invite request
    );

    @Operation(summary = "내가 받은 초대장 목록 조회", description = "유저 본인에게 온 대기 중인 프로젝트 초대장 목록을 조회합니다.")
    ApiResponse<List<ProjectMemberResponseDto.InvitationInfo>> getMyInvitations(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "프로젝트 초대 수락/거절", description = "받은 초대에 대해 수락(true) 또는 거절(false)을 처리합니다.")
    ApiResponse<Void> respondToInvitation(
            @PathVariable Long inviteId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @RequestBody ProjectMemberRequestDto.Respond request
    );

    @Operation(summary = "프로젝트 멤버 조회", description = "해당 프로젝트에 가입된 멤버 목록을 조회합니다.")
    ApiResponse<List<ProjectMemberResponseDto.MemberInfo>> getMembers(
            @PathVariable Long projectId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "프로젝트 멤버 권한 변경", description = "프로젝트 리더가 특정 멤버의 권한을 변경합니다.")
    ApiResponse<Void> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long targetUserId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            @RequestBody ProjectMemberRequestDto.UpdateRole request
    );

    @Operation(summary = "프로젝트 멤버 탈퇴 및 강퇴", description = "리더가 멤버를 강퇴하거나, 본인이 프로젝트에서 스스로 탈퇴합니다.")
    ApiResponse<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long targetUserId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );
}
