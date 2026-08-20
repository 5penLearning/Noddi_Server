package com._penLearning.Noddi.domain.team.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.team.code.TeamApi;
import com._penLearning.Noddi.domain.team.dto.TeamRequestDto;
import com._penLearning.Noddi.domain.team.dto.TeamResponseDto;
import com._penLearning.Noddi.domain.team.service.TeamCommandService;
import com._penLearning.Noddi.domain.team.service.TeamQueryService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TeamController implements TeamApi {

    private final TeamCommandService teamCommandService;
    private final TeamQueryService teamQueryService;

    @Override
    @PostMapping("/projects/{projectId}/teams")
    public ApiResponse<Long> createTeam(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamRequestDto.Create request) {

        Long teamId = teamCommandService.createTeam(
                projectId,
                authMember.getUserId(),
                request.getName(),
                request.getDescription()
        );
        return ApiResponse.onSuccess("팀이 성공적으로 생성되었습니다.", teamId);
    }

    @Override
    @PostMapping("/teams/{teamId}/members/invite")
    public ApiResponse<Void> inviteTeamMember(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamRequestDto.InviteMember request) {

        teamCommandService.inviteTeamMember(teamId, authMember.getUserId(), request.getTargetUserId());
        return ApiResponse.onSuccess("팀 멤버 초대장이 발송되었습니다.", null);
    }

    @Override
    @PostMapping("/teams/invitations/{inviteId}/respond")
    public ApiResponse<Void> respondToInvite(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamRequestDto.RespondInvite request) {

        teamCommandService.respondToInvite(inviteId, authMember.getUserId(), request.getIsAccepted());
        return ApiResponse.onSuccess("초대 응답이 정상적으로 처리되었습니다.", null);
    }

    @Override
    @PatchMapping("/teams/{teamId}")
    public ApiResponse<Void> updateTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamRequestDto.UpdateInfo request) {
        teamCommandService.updateTeam(teamId, authMember.getUserId(), request);
        return ApiResponse.onSuccess("팀 정보가 성공적으로 수정되었습니다.", null);
    }

    @Override
    @DeleteMapping("/teams/{teamId}")
    public ApiResponse<Void> deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember) {
        teamCommandService.deleteTeam(teamId, authMember.getUserId());
        return ApiResponse.onSuccess("팀이 성공적으로 삭제되었습니다.", null);
    }

    @Override
    @PatchMapping("/teams/{teamId}/members/{targetUserId}/role")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid TeamRequestDto.UpdateRole request) {
        teamCommandService.updateMemberRole(teamId, authMember.getUserId(), targetUserId, request.getRole());
        return ApiResponse.onSuccess("팀 멤버 권한이 성공적으로 변경되었습니다.", null);
    }

    @Override
    @DeleteMapping("/teams/{teamId}/members/{targetUserId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal AuthMember authMember) {
        teamCommandService.removeMember(teamId, authMember.getUserId(), targetUserId);
        return ApiResponse.onSuccess("팀 멤버가 성공적으로 삭제/탈퇴되었습니다.", null);
    }

    @Override
    @GetMapping("/projects/{projectId}/teams")
    public ApiResponse<List<TeamResponseDto.ProjectTeamInfo>> getTeamsByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember authMember) {
        List<TeamResponseDto.ProjectTeamInfo> response = teamQueryService.getTeamsByProject(projectId, authMember.getUserId());
        return ApiResponse.onSuccess("프로젝트 내 팀 목록 조회에 성공했습니다.", response);
    }

    @Override
    @GetMapping("/users/me/teams")
    public ApiResponse<List<TeamResponseDto.TeamInfo>> getMyTeams(
            @AuthenticationPrincipal AuthMember authMember) {
        List<TeamResponseDto.TeamInfo> response = teamQueryService.getMyTeams(authMember.getUserId());
        return ApiResponse.onSuccess("내 팀 목록 조회에 성공했습니다.", response);
    }

    @Override
    @GetMapping("/teams/{teamId}/members")
    public ApiResponse<List<TeamResponseDto.MemberInfo>> getTeamMembers(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember) {
        List<TeamResponseDto.MemberInfo> response = teamQueryService.getTeamMembers(teamId, authMember.getUserId());
        return ApiResponse.onSuccess("팀 멤버 목록 조회에 성공했습니다.", response);
    }

    @Override
    @GetMapping("/users/me/teams/invitations")
    public ApiResponse<List<TeamResponseDto.InvitationInfo>> getMyInvitations(
            @AuthenticationPrincipal AuthMember authMember) {
        List<TeamResponseDto.InvitationInfo> response = teamQueryService.getMyInvitations(authMember.getUserId());
        return ApiResponse.onSuccess("받은 팀 초대장 목록 조회에 성공했습니다.", response);
    }
}