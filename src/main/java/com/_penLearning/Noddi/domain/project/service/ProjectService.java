package com._penLearning.Noddi.domain.project.service;

import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.dto.ProjectRequestDto;
import com._penLearning.Noddi.domain.project.dto.ProjectResponseDto;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.project.repository.ProjectInviteRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final TeamMemberRepository teamMemberRepository;

    // 프로젝트 생성
    @Transactional
    public ProjectResponseDto.Info createProject(Long userId, ProjectRequestDto.Create request) {
        // 1. JWT에서 추출한 userId로 유저 객체 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        // 2. 엔티티 생성 (유저의 소속 조직 자동 매핑)
        Project project = Project.create(
                request.getName(),
                request.getDescription(),
                user.getOrganization(),
                user
        );

        // 3. DB 저장 및 반환
        Project savedProject = projectRepository.save(project);

        // 4. 프로젝트 생성한 유저를 리더로 등록
        ProjectMember leaderMember = ProjectMember.create(
                project,
                user,
                ProjectRole.LEADER
        );

        projectMemberRepository.save(leaderMember);
        return ProjectResponseDto.Info.from(savedProject);
    }

    // 소속 조직의 프로젝트 목록 조회
    public List<ProjectResponseDto.Info> getProjectsByOrganization(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        // 소속 조직 기반으로 프로젝트 목록 조회 (Fetch Join 적용됨)
        List<Project> projects = projectRepository.findAllByOrganization(user.getOrganization());

        return projects.stream()
                .map(ProjectResponseDto.Info::from)
                .collect(Collectors.toList());
    }
    // 프로젝트 정보 수정(리더만 가능)
    @Transactional
    public void updateProject(Long projectId, Long requesterId, ProjectRequestDto.Update request) {
        Project project = getProjectOrThrow(projectId);
        validateProjectLeader(project, requesterId); // 리더 권한 검증 재사용

        project.updateProjectInfo(request.getName(), request.getDescription());
    }

    // 프로젝트 삭제 (리더만 가능)
    @Transactional
    public void deleteProject(Long projectId, Long requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));

        // 1. 요청자가 해당 프로젝트의 LEADER인지 검증
        validateProjectLeader(project, requesterId);

        // FK 제약조건을 고려해 팀의 하위 데이터부터 삭제한다.
        teamInviteRepository.bulkDeleteByProject(project);
        teamMemberRepository.bulkDeleteByProject(project);
        teamRepository.bulkDeleteByProject(project);
        projectInviteRepository.deleteBulkByProject(project);
        projectMemberRepository.bulkDeleteByProject(project);

        // 3. 프로젝트 삭제
        projectRepository.delete(project);
    }
    // 리더 권한 검증 헬퍼 메서드
    private void validateProjectLeader(Project project, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        ProjectMember member = projectMemberRepository.findByProjectAndUser(project, user)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND));

        if (member.getRole() != ProjectRole.LEADER) {
            throw new GeneralException(ProjectErrorCode.NOT_PROJECT_LEADER);
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }
}
