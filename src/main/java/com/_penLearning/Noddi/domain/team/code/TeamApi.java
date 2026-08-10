package com._penLearning.Noddi.domain.team.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.team.dto.TeamRequestDto;
import com._penLearning.Noddi.domain.team.dto.TeamResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Team API", description = "팀 및 팀 멤버 관리 API")
public interface TeamApi {

    @Operation(summary = "팀 생성", description = "지정된 프로젝트 내에 새로운 팀을 생성합니다. (프로젝트 정식 멤버만 가능)")
    ApiResponse<Long> createTeam(
            @PathVariable Long projectId,
            @Parameter(hidden = true) AuthMember authMember, // 실제 인증 객체 타입으로 교체 (예: AuthMember)
            @RequestBody TeamRequestDto.Create request
    );

    @Operation(summary = "팀 멤버 초대", description = "팀 리더가 동일한 프로젝트 내의 정식 멤버를 팀으로 초대합니다.")
    ApiResponse<Void> inviteTeamMember(
            @PathVariable Long teamId,
            @Parameter(hidden = true) AuthMember authMember,
            @RequestBody TeamRequestDto.InviteMember request
    );

    @Operation(summary = "팀 초대 응답", description = "팀 초대를 수락하거나 거절합니다.")
    ApiResponse<Void> respondToInvite(
            @PathVariable Long inviteId,
            @Parameter(hidden = true) AuthMember authMember,
            @RequestBody TeamRequestDto.RespondInvite request
    );

    @Operation(summary = "팀 정보 수정", description = "팀 리더가 팀의 이름과 설명을 수정합니다.")
    ApiResponse<Void> updateTeam(@PathVariable Long teamId,
                                 @Parameter(hidden = true) AuthMember authMember,
                                 @RequestBody TeamRequestDto.UpdateInfo request);

    @Operation(summary = "팀 삭제", description = "팀 리더가 팀을 삭제합니다. 관련된 초대장과 멤버 정보도 함께 삭제됩니다.")
    ApiResponse<Void> deleteTeam(@PathVariable Long teamId,
                                 @Parameter(hidden = true) AuthMember authMember);

    @Operation(summary = "팀 멤버 권한 변경", description = "팀 리더가 멤버의 권한(LEADER, MEMBER)을 변경합니다.")
    ApiResponse<Void> updateMemberRole(@PathVariable Long teamId,
                                       @PathVariable Long targetUserId,
                                       @Parameter(hidden = true) AuthMember authMember,
                                       @RequestBody TeamRequestDto.UpdateRole request);

    @Operation(summary = "팀 멤버 강퇴 및 탈퇴", description = "팀 리더가 멤버를 강퇴하거나, 본인이 팀에서 탈퇴합니다.")
    ApiResponse<Void> removeMember(@PathVariable Long teamId,
                                   @PathVariable Long targetUserId,
                                   @Parameter(hidden = true) AuthMember authMember);

    @Operation(summary = "프로젝트 내 팀 목록 조회", description = "특정 프로젝트에 속한 모든 팀의 목록을 조회합니다. (프로젝트 정식 멤버만 가능)")
    ApiResponse<List<TeamResponseDto.ProjectTeamInfo>> getTeamsByProject(
            @PathVariable Long projectId,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "내 팀 목록 조회", description = "내가 속해 있는 팀들의 목록을 조회합니다.")
    ApiResponse<List<TeamResponseDto.TeamInfo>> getMyTeams(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "팀 멤버 목록 조회", description = "특정 팀에 소속된 정식 멤버들의 목록을 조회합니다. (해당 팀원만 조회 가능)")
    ApiResponse<List<TeamResponseDto.MemberInfo>> getTeamMembers(
            @PathVariable Long teamId,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "받은 팀 초대장 목록 조회", description = "내가 받은 대기 중(PENDING)인 팀 초대장 목록을 조회합니다.")
    ApiResponse<List<TeamResponseDto.InvitationInfo>> getMyInvitations(
            @Parameter(hidden = true) AuthMember authMember
    );
}
