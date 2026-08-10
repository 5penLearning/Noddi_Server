package com._penLearning.Noddi.domain.team.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // 특정 프로젝트에 속한 모든 팀 조회
    @Query("select t from Team t join fetch t.createdBy where t.project = :project")
    List<Team> findAllByProject(@Param("project") Project project);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Team t WHERE t.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);

    void deleteAllByProject(Project project);
}
