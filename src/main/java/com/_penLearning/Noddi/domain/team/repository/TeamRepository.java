package com._penLearning.Noddi.domain.team.repository;

import com._penLearning.Noddi.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByProject_ProjectId(Long projectId);

    // 같은 조직 내 팀 전체 조회 (타 팀 Q&A 대상 리스트용)
    @Query("SELECT t FROM Team t WHERE t.project.organization.organizationId = :organizationId")
    List<Team> findAllByOrganizationId(@Param("organizationId") Long organizationId);
}
