package com._penLearning.Noddi.domain.team.service;

import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.entity.JoinStatus;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.dto.TeamResponseDto;
import com._penLearning.Noddi.domain.team.entity.InviteStatus;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamQueryService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public List<TeamResponseDto.ProjectTeamInfo> getTeamsByProject(Long projectId, Long requesterId) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        // 프로젝트 정식 멤버인지 검증 (외부인 접근 차단)
        validateProjectMember(project, requester);

        return teamRepository.findAllByProject(project).stream()
                .map(TeamResponseDto.ProjectTeamInfo::from)
                .toList();
    }

    // 내가 속한 팀 목록 조회
    public List<TeamResponseDto.TeamInfo> getMyTeams(Long userId) {
        User user = getUserOrThrow(userId);

        return teamMemberRepository.findAllByUserWithTeam(user).stream()
                .map(TeamResponseDto.TeamInfo::from)
                .toList();
    }

    // 특정 팀의 멤버 목록 조회
    public List<TeamResponseDto.MemberInfo> getTeamMembers(Long teamId, Long requesterId) {
        Team team = getTeamOrThrow(teamId);
        User requester = getUserOrThrow(requesterId);

        // 팀 멤버 목록은 해당 팀에 소속된 유저만 조회 가능 (외부인 차단)
        validateTeamMembership(team, requester);

        return teamMemberRepository.findAllByTeamWithUser(team).stream()
                .map(TeamResponseDto.MemberInfo::from)
                .toList();
    }

    // 내가 받은 팀 초대장 목록 조회
    public List<TeamResponseDto.InvitationInfo> getMyInvitations(Long userId) {
        User user = getUserOrThrow(userId);

        // PENDING 상태인 초대장만, Team 정보와 함께 페치 조인으로 가져옴
        return teamInviteRepository.findByInviteeAndStatusWithTeam(user, InviteStatus.PENDING).stream()
                .map(TeamResponseDto.InvitationInfo::from)
                .toList();
    }

    // --- 내부 검증 헬퍼 메서드 ---

    private void validateProjectMember(Project project, User user) {
        ProjectMember projectMember = projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.NOT_PROJECT_MEMBER));

        if (projectMember.getStatus() != JoinStatus.JOINED) {
            throw new GeneralException(TeamErrorCode.NOT_PROJECT_MEMBER);
        }
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
                throw new IllegalArgumentException("팀의 마지막 리더는 탈퇴하거나 권한을 강등할 수 없습니다.");
            }
        }
    }

    // 팀에 속해 있는지만 확인하는 검증 로직
    private void validateTeamMembership(Team team, User user) {
        if (!teamMemberRepository.existsByTeamAndUser(team, user)) {
            // 필요시 TeamErrorCode.NOT_TEAM_MEMBER 등으로 교체
            throw new GeneralException(TeamErrorCode.TEAM_NOT_FOUND);
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
        // Auth/User 예외 코드 활용 권장
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
