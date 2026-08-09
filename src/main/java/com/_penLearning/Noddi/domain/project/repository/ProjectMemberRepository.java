package com._penLearning.Noddi.domain.project.repository;

import com._penLearning.Noddi.domain.project.dto.ProjectMemberResponseDto;
import com._penLearning.Noddi.domain.project.entity.JoinStatus;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // 특정 프로젝트에 특정 유저가 존재하는지 확인 (중복 초대 방지)
    boolean existsByProjectAndUser(Project project, User user);

    // 단건 조회 (권한 검증 및 탈퇴 로직에 사용)
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    // 프로젝트의 정식 멤버 목록 조회 (상태가 JOINED인 사람만)
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.user WHERE pm.project = :project AND pm.status = 'JOINED'")
    List<ProjectMember> findJoinedMembersByProject(@Param("project") Project project);

    // 내가 받은 초대장 목록 조회 (N+1 방지를 위해 Project 패치 조인)
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.project WHERE pm.user = :user AND pm.status = 'INVITED'")
    List<ProjectMember> findInvitationsByUser(@Param("user") User user);

    long countByProjectAndRoleAndStatus(Project project, ProjectRole role, JoinStatus status);

    void deleteAllByProject(Project project);
}
