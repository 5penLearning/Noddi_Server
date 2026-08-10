package com._penLearning.Noddi.domain.project.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.project.code.ProjectMemberApi;
import com._penLearning.Noddi.domain.project.dto.ProjectMemberRequestDto;
import com._penLearning.Noddi.domain.project.dto.ProjectMemberResponseDto;
import com._penLearning.Noddi.domain.project.service.ProjectMemberService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectMemberController implements ProjectMemberApi {

    private final ProjectMemberService projectMemberService;

    @Override
    @PostMapping("/{projectId}/members/invite")
    public ApiResponse<Void> inviteUser(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid ProjectMemberRequestDto.Invite request) {

        projectMemberService.inviteUser(projectId, authMember.getUserId(), request.getTargetUserId());
        return ApiResponse.onSuccess("프로젝트 초대가 발송되었습니다.");
    }

    @Override
    @GetMapping("/invitations")
    public ApiResponse<List<ProjectMemberResponseDto.InvitationInfo>> getMyInvitations(
            @AuthenticationPrincipal AuthMember authMember) {

        List<ProjectMemberResponseDto.InvitationInfo> response = projectMemberService.getMyInvitations(authMember.getUserId());

        return ApiResponse.onSuccess("받은 초대장 목록 조회에 성공했습니다.", response);
    }

    @Override
    @PostMapping("/invitations/{inviteId}/respond")
    public ApiResponse<Void> respondToInvitation(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid ProjectMemberRequestDto.Respond request) {

        projectMemberService.respondToInvitation(inviteId, authMember.getUserId(), request.getIsAccepted());
        String message = request.getIsAccepted() ? "초대를 수락하여 프로젝트에 가입되었습니다." : "초대를 거절했습니다.";

        return ApiResponse.onSuccess(message);
    }

    @Override
    @GetMapping("/{projectId}/members")
    public ApiResponse<List<ProjectMemberResponseDto.MemberInfo>> getMembers(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember authMember) {

        // 1. 서비스 로직 호출 (결과물은 DTO 리스트로 받음)
        List<ProjectMemberResponseDto.MemberInfo> response =
                projectMemberService.getMembers(projectId, authMember.getUserId());
        // 2. 일관된 응답 포맷(ApiResponse)으로 감싸서 리턴
        return ApiResponse.onSuccess("프로젝트 멤버 목록 조회에 성공했습니다.", response);
    }

    @Override
    @PatchMapping("/{projectId}/members/{targetUserId}/role")
    public ApiResponse<Void> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid ProjectMemberRequestDto.UpdateRole request) {

        projectMemberService.updateMemberRole(projectId, authMember.getUserId(), targetUserId, request.getNewRole());

        return ApiResponse.onSuccess("멤버 권한이 성공적으로 변경되었습니다.");
    }

    @Override
    @DeleteMapping("/{projectId}/members/{targetUserId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal AuthMember authMember) {

        projectMemberService.removeMember(projectId, authMember.getUserId(), targetUserId);

        return ApiResponse.onSuccess("프로젝트 멤버 탈퇴 처리가 완료되었습니다.");
    }
}
