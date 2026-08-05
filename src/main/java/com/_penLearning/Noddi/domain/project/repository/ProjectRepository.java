package com._penLearning.Noddi.domain.project.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOrganization_OrganizationId(Long organizationId);
}
