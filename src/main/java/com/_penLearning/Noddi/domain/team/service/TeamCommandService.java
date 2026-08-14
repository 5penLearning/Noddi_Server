package com._penLearning.Noddi.domain.team.service;

import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.dto.TeamRequestDto;
import com._penLearning.Noddi.domain.team.entity.*;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamCommandService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TeamInviteExpirationService teamInviteExpirationService;
    private final TeamPageRepository teamPageRepository;

    // 팀 생성 (생성자는 자동으로 LEADER 역할 부여)
    @Transactional
    public Long createTeam(Long projectId, Long requesterId, String teamName, String description) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        // 요청자가 해당 프로젝트의 정식 멤버(JOINED)인지 팩트 체크
        validateProjectMember(project, requester);

        Team team = Team.builder()
                .project(project)
                .name(teamName)
                .description(description)
                .createdBy(requester)
                .build();
        teamRepository.save(team);

        TeamMember leader = TeamMember.builder()
                .team(team)
                .user(requester)
                .role(TeamRole.LEADER)
                .build();
        teamMemberRepository.save(leader);

        return team.getTeamId();
    }

    // 팀 멤버 초대장 발송
    @Transactional
    public void inviteTeamMember(Long teamId, Long requesterId, Long targetUserId) {
        Team team = getTeamOrThrow(teamId);
        User requester = getUserOrThrow(requesterId);
        User targetUser = getUserOrThrow(targetUserId);

        validateTeamLeader(team, requester);
        validateProjectMember(team.getProject(), targetUser);

        if (teamMemberRepository.existsByTeamAndUser(team, targetUser)) {
            throw new GeneralException(TeamErrorCode.ALREADY_TEAM_MEMBER);
        }

        if (teamInviteRepository.existsByTeamAndInviteeAndStatus(team, targetUser, com._penLearning.Noddi.domain.team.entity.InviteStatus.PENDING)) {
            throw new GeneralException(TeamErrorCode.ALREADY_INVITED);
        }

        TeamInvite invite = TeamInvite.builder()
                .team(team)
                .inviter(requester)
                .invitee(targetUser)
                .build();
        teamInviteRepository.save(invite);
    }

    // 팀 초대 응답
    @Transactional
    public void respondToInvite(Long inviteId, Long userId, boolean isAccepted) {
        TeamInvite invite = teamInviteRepository.findByIdWithTeam(inviteId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.INVITE_NOT_FOUND));

        if (!invite.getInvitee().getUserId().equals(userId)) {
            throw new GeneralException(TeamErrorCode.NOT_INVITEE);
        }

        if (invite.getStatus() != com._penLearning.Noddi.domain.team.entity.InviteStatus.PENDING) {
            throw new GeneralException(TeamErrorCode.INVITE_NOT_PENDING);
        }

        if (invite.isExpired(7)) {
            teamInviteExpirationService.expire(inviteId);
            throw new GeneralException(TeamErrorCode.INVITE_NOT_PENDING);
        }

        if (isAccepted) {

            validateProjectMember(
                    invite.getTeam().getProject(),
                    invite.getInvitee()
            );

            if (teamMemberRepository.existsByTeamAndUser(
                    invite.getTeam(),
                    invite.getInvitee()
            )) {
                throw new GeneralException(TeamErrorCode.ALREADY_TEAM_MEMBER);
            }

            invite.accept();

            TeamMember newMember = TeamMember.builder()
                    .team(invite.getTeam())
                    .user(invite.getInvitee())
                    .role(TeamRole.MEMBER)
                    .build();
            teamMemberRepository.save(newMember);
        } else {
            invite.reject();
        }
    }

    // 팀 정보 수정
    @Transactional
    public void updateTeam(Long teamId, Long requesterId, TeamRequestDto.UpdateInfo request) {
        Team team = getTeamOrThrow(teamId);
        User requester = getUserOrThrow(requesterId);

        validateTeamLeader(team, requester); // 리더 권한 검증

        team.updateTeamInfo(request.getName(), request.getDescription());
    }

    // 팀 삭제
    @Transactional
    public void deleteTeam(Long teamId, Long requesterId) {
        Team team = getTeamOrThrow(teamId);
        User requester = getUserOrThrow(requesterId);

        validateTeamLeader(team, requester);

        // 하위 데이터 일괄 삭제 (FK 제약조건 방어)
        teamPageRepository.bulkDeleteByTeam(team);
        teamInviteRepository.deleteAllByTeam(team);
        teamMemberRepository.deleteAllByTeam(team);

        // 팀 삭제
        teamRepository.delete(team);
    }

    // 멤버 권한 변경
    @Transactional
    public void updateMemberRole(Long teamId, Long requesterId, Long targetUserId, TeamRole newRole) {
        Team team = getTeamOrThrow(teamId);
        User requester = getUserOrThrow(requesterId);
        User targetUser = getUserOrThrow(targetUserId);

        validateTeamLeader(team, requester);

        TeamMember targetMember = teamMemberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND));

        // 자신이 리더인데 MEMBER로 강등하려는 경우 방어
        if (targetMember.getRole() == TeamRole.LEADER && newRole == TeamRole.MEMBER) {
            validateNotLastLeader(team, targetMember);
        }

        targetMember.updateRole(newRole);
    }

    // 팀 멤버 탈퇴 / 강퇴
    @Transactional
    public void removeMember(Long teamId, Long requesterId, Long targetUserId) {
        Team team = getTeamOrThrow(teamId);
        User targetUser = getUserOrThrow(targetUserId);

        TeamMember targetMember = teamMemberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));

        // 본인이 스스로 탈퇴하는 경우가 아니라면, 요청자가 리더인지 검증
        if (!requesterId.equals(targetUserId)) {
            User requester = getUserOrThrow(requesterId);
            validateTeamLeader(team, requester);
        }

        validateNotLastLeader(team, targetMember); // 마지막 리더 탈퇴 방어

        teamMemberRepository.delete(targetMember);
    }

    // --- 내부 검증 헬퍼 메서드 ---

    private void validateProjectMember(Project project, User user) {
        projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.NOT_PROJECT_MEMBER));
    }

    private void validateTeamLeader(Team team, User user) {
        validateProjectMember(team.getProject(), user);

        TeamMember teamMember =
                teamMemberRepository.findByTeamAndUser(team, user)
                        .orElseThrow(() ->
                                new GeneralException(TeamErrorCode.NOT_TEAM_LEADER));

        if (teamMember.getRole() != TeamRole.LEADER) {
            throw new GeneralException(TeamErrorCode.NOT_TEAM_LEADER);
        }
    }

    // 마지막 리더인지 검증하는 헬퍼 메서드
    private void validateNotLastLeader(Team team, TeamMember targetMember) {
        if (targetMember.getRole() == TeamRole.LEADER) {
            long leaderCount = teamMemberRepository.countByTeamAndRole(team, TeamRole.LEADER);
            if (leaderCount <= 1) {
                throw new GeneralException(TeamErrorCode.CANNOT_REMOVE_LAST_LEADER);
            }
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
