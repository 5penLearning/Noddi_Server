package com._penLearning.Noddi.domain.project.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    // 1. 특정 프로젝트에 특정 유저가 존재하는지 확인 (중복 초대 방지 등)
    boolean existsByProjectAndUser(Project project, User user);

    // 2. 단건 조회 (권한 검증 및 탈퇴 로직에 사용)
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    // 3. 프로젝트의 멤버 목록 조회 (N+1 방지)
    // 팩트: 상태 검증이 필요 없으므로 기존 findJoinedMembersByProject를 삭제하고 이것으로 통합
    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.user WHERE pm.project = :project")
    List<ProjectMember> findAllByProjectWithUser(@Param("project") Project project);

    // 4. 특정 권한을 가진 멤버 수 카운트 (마지막 리더 검증용)
    // 팩트: Status 조건이 포함되었던 countByProjectAndRoleAndStatus 쿼리 대체
    long countByProjectAndRole(Project project, ProjectRole role);

    // 5. 프로젝트 삭제 시 멤버 일괄 삭제 (벌크 연산)
    // 팩트: N+1 문제를 유발하는 기존 deleteAllByProject 메서드는 완전 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProjectMember pm WHERE pm.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);
}
