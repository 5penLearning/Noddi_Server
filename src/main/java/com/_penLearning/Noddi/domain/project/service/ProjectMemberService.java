package com._penLearning.Noddi.domain.project.service;

import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.dto.ProjectMemberResponseDto;
import com._penLearning.Noddi.domain.project.entity.*;
import com._penLearning.Noddi.domain.project.repository.ProjectInviteRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectInviteExpirationService projectInviteExpirationService;

    @Value("${scheduler.invite.valid-days:7}")
    private int inviteValidDays;

    // 관리자가 유저를 초대
    @Transactional
    public void inviteUser(Long projectId, Long requesterId, Long targetUserId) {
        Project project = getProjectOrThrow(projectId);
        User targetUser = getUserOrThrow(targetUserId);

        validateProjectLeader(project, requesterId);

        if (!project.getOrganization().getOrganizationId()
                .equals(targetUser.getOrganization().getOrganizationId())) {
            throw new GeneralException(ProjectErrorCode.NOT_SAME_ORGANIZATION);
        }

        // ProjectMember가 존재하면 이미 가입된 사용자다.
        if (projectMemberRepository.existsByProjectAndUser(project, targetUser)) {
            throw new GeneralException(ProjectErrorCode.ALREADY_PROJECT_MEMBER);
        }

        // 2. 중복 초대 방어
        if (projectInviteRepository.existsByProjectAndInviteeAndStatus(project, targetUser, InviteStatus.PENDING)) {
            throw new GeneralException(ProjectErrorCode.ALREADY_INVITED_USER);
        }

        User requester = getUserOrThrow(requesterId);
        ProjectInvite invite = ProjectInvite.builder()
                .project(project)
                .inviter(requester)
                .invitee(targetUser)
                .build();
        projectInviteRepository.save(invite);
    }

    // 받은 초대장 조회
    public List<ProjectMemberResponseDto.InvitationInfo> getMyInvitations(Long userId) {
        User user = getUserOrThrow(userId);

        return projectInviteRepository.findByInviteeAndStatusWithProject(user, InviteStatus.PENDING).stream()
                .map(ProjectMemberResponseDto.InvitationInfo::from)
                .toList();
    }

    // 초대장 응답 처리
    @Transactional
    public void respondToInvitation(Long inviteId, Long userId, boolean isAccepted) {
        ProjectInvite invite = projectInviteRepository.findByIdWithProject(inviteId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.INVITE_NOT_FOUND));

        if (!invite.getInvitee().getUserId().equals(userId)) {
            throw new GeneralException(ProjectErrorCode.NOT_INVITEE);
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new GeneralException(ProjectErrorCode.INVITE_NOT_PENDING);
        }

        // 스케줄러 실행 전이라도 응답 시점에 만료 여부를 다시 검증한다.
        if (invite.isExpired(inviteValidDays)) {
            projectInviteExpirationService.expire(inviteId);
            throw new GeneralException(ProjectErrorCode.INVITE_EXPIRED);
        }

        if (isAccepted) {
            if (!invite.getProject().getOrganization().getOrganizationId()
                    .equals(invite.getInvitee().getOrganization().getOrganizationId())) {
                throw new GeneralException(ProjectErrorCode.NOT_SAME_ORGANIZATION);
            }

            if (projectMemberRepository.existsByProjectAndUser(invite.getProject(), invite.getInvitee())) {
                throw new GeneralException(ProjectErrorCode.ALREADY_PROJECT_MEMBER);
            }

            invite.accept();
            ProjectMember newMember = ProjectMember.builder()
                    .project(invite.getProject())
                    .user(invite.getInvitee())
                    .role(ProjectRole.MEMBER)
                    .build();
            projectMemberRepository.save(newMember);
        } else {
            invite.reject();
        }
    }

    // 프로젝트 멤버 조회
    public List<ProjectMemberResponseDto.MemberInfo> getMembers(Long projectId, Long requesterId) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        getProjectMemberOrThrow(project, requester);

        return projectMemberRepository.findAllByProjectWithUser(project).stream()
                .map(ProjectMemberResponseDto.MemberInfo::from)
                .toList();
    }

    // 멤버 권한 변경 (리더만 가능)
    @Transactional
    public void updateMemberRole(Long projectId, Long requesterId, Long targetUserId, ProjectRole newRole) {
        Project project = getProjectOrThrow(projectId);
        validateProjectLeader(project, requesterId);

        User targetUser = getUserOrThrow(targetUserId);
        ProjectMember targetMember = getProjectMemberOrThrow(project, targetUser);

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
            validateProjectLeader(project, requesterId);
        }

        // 마지막 리더 탈퇴 방어
        validateNotLastLeader(project, targetMember);
        projectMemberRepository.delete(targetMember);
    }

    // --- 내부 검증 및 조회 편의 메서드 ---

    private void validateProjectLeader(Project project, Long userId) {
        User user = getUserOrThrow(userId);
        ProjectMember member = getProjectMemberOrThrow(project, user);

        if (member.getRole() != ProjectRole.LEADER) {
            throw new GeneralException(ProjectErrorCode.NOT_PROJECT_LEADER);
        }
    }

    // 마지막 리더 탈퇴 방지 위한 헬퍼 메서드
    private void validateNotLastLeader(Project project, ProjectMember targetMember) {
        if (targetMember.getRole() == ProjectRole.LEADER) {
            long leaderCount = projectMemberRepository.countByProjectAndRole(project, ProjectRole.LEADER);
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
