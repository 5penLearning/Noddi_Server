package com._penLearning.Noddi.domain.project.service;

import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.dto.ProjectMemberResponseDto;
import com._penLearning.Noddi.domain.project.entity.JoinStatus;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
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
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // 관리자가 유저를 초대
    @Transactional
    public void inviteUser(Long projectId, Long requesterId, Long targetUserId) {
        Project project = getProjectOrThrow(projectId);
        validateLeaderPermission(project, requesterId); // 리더만 초대 가능

        User targetUser = getUserOrThrow(targetUserId);

        if (!project.getOrganization().getOrganizationId().equals(targetUser.getOrganization().getOrganizationId())) {
            throw new GeneralException(ProjectErrorCode.NOT_SAME_ORGANIZATION);
        }

        projectMemberRepository.findByProjectAndUser(project, targetUser)
                .ifPresent(existingMember -> {
                    if (existingMember.getStatus() == JoinStatus.JOINED) {
                        throw new GeneralException(ProjectErrorCode.ALREADY_PROJECT_MEMBER);
                    } else if (existingMember.getStatus() == JoinStatus.INVITED) {
                        throw new GeneralException(ProjectErrorCode.ALREADY_INVITED_USER);
                    }
                });

        // 데이터가 없을 경우에만 새로운 초대 레코드 생성
        ProjectMember newMember = ProjectMember.create(project, targetUser, ProjectRole.MEMBER, JoinStatus.INVITED);
        projectMemberRepository.save(newMember);
    }

    // 받은 초대장 조회
    public List<ProjectMemberResponseDto .InvitationInfo> getMyInvitations(Long userId) {
        User user = getUserOrThrow(userId);
        return projectMemberRepository.findInvitationsByUser(user).stream()
                .map(ProjectMemberResponseDto.InvitationInfo::from)
                .toList();
    }

    // 초대장 응답 처리
    @Transactional
    public void respondToInvitation(Long projectId, Long userId, boolean isAccepted) {
        Project project = getProjectOrThrow(projectId);
        User user = getUserOrThrow(userId);

        ProjectMember invitation = getProjectMemberOrThrow(project, user);

        // 이미 가입했거나 다른 상태인지 팩트 체크
        if (invitation.getStatus() != JoinStatus.INVITED) {
            throw new GeneralException(ProjectErrorCode.INVALID_PROJECT_REQUEST);
        }

        if (isAccepted) {
            invitation.acceptInvitation(); // 상태를 JOINED로 UPDATE (JPA 더티 체킹)
        } else {
            projectMemberRepository.delete(invitation); // 거절 시 데이터를 삭제하여 다시 초대받을 수 있게 처리
        }
    }

    // 프로젝트 멤버 조회
    public List<ProjectMemberResponseDto.MemberInfo> getMembers(
            Long projectId, Long requesterId
    ) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        ProjectMember requesterMember =
                getProjectMemberOrThrow(project, requester);

        if (requesterMember.getStatus() != JoinStatus.JOINED) {
            throw new GeneralException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }

        return projectMemberRepository.findJoinedMembersByProject(project).stream()
                .map(ProjectMemberResponseDto.MemberInfo::from)
                .toList();
    }

    // 멤버 권한 변경 (리더만 가능)
    @Transactional
    public void updateMemberRole(Long projectId, Long requesterId, Long targetUserId, ProjectRole newRole) {
        Project project = getProjectOrThrow(projectId);
        validateLeaderPermission(project, requesterId);

        User targetUser = getUserOrThrow(targetUserId);
        ProjectMember targetMember = getProjectMemberOrThrow(project, targetUser);

        // 방어 로직: 아직 수락하지 않은(INVITED) 사람은 권한 변경 불가
        if (targetMember.getStatus() != JoinStatus.JOINED) {
            throw new GeneralException(ProjectErrorCode.INVALID_PROJECT_REQUEST);
        }
        // 방어 로직: 마지막 리더는 권한 강등 불가
        if (targetMember.getRole() == ProjectRole.LEADER && newRole == ProjectRole.MEMBER) {
            validateNotLastLeader(project, targetMember);
        }
        targetMember.updateRole(newRole);
    }

    // 프로젝트 탈퇴 및 추방
    @Transactional
    public void removeMember(Long projectId, Long requesterId, Long targetUserId) {
        Project project = getProjectOrThrow(projectId);
        User targetUser = getUserOrThrow(targetUserId);
        ProjectMember targetMember = getProjectMemberOrThrow(project, targetUser);

        // 본인이 스스로 탈퇴하는 경우이거나, 리더가 남을 강퇴하는 경우만 허용
        if (!requesterId.equals(targetUserId)) {
            validateLeaderPermission(project, requesterId);
        }

        // 마지막 리더 탈퇴 방어
        validateNotLastLeader(project, targetMember);
        projectMemberRepository.delete(targetMember);
    }

    // --- 내부 검증 및 조회 편의 메서드 ---

    // 리더인지 확인
    private void validateLeaderPermission(Project project, Long userId) {
        User user = getUserOrThrow(userId);
        ProjectMember member = getProjectMemberOrThrow(project, user);

        if (member.getRole() != ProjectRole.LEADER) {
            throw new GeneralException(ProjectErrorCode.NOT_PROJECT_LEADER);
        }
    }

    // 마지막 리더 탈퇴 방지 위한 헬퍼 메서드
    private void validateNotLastLeader(Project project, ProjectMember targetMember) {
        if (targetMember.getRole() == ProjectRole.LEADER) {
            long leaderCount = projectMemberRepository.countByProjectAndRoleAndStatus(project, ProjectRole.LEADER, JoinStatus.JOINED);
            if (leaderCount <= 1) {
                throw new GeneralException(ProjectErrorCode.CANNOT_REMOVE_LAST_LEADER);
            }
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private ProjectMember getProjectMemberOrThrow(Project project, User user) {
        return projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }
}
