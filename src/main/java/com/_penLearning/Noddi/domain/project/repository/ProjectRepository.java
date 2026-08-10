package com._penLearning.Noddi.domain.project.repository;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // N+1 문제 방지를 위해 createdBy(User)를 Fetch Join으로 함께 조회
    @Query("SELECT p FROM Project p JOIN FETCH p.createdBy WHERE p.organization = :organization")
    List<Project> findAllByOrganization(@Param("organization") Organization organization);
}
