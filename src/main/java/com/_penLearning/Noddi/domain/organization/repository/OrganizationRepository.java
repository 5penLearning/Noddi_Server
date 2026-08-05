package com._penLearning.Noddi.domain.organization.repository;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByEmailDomain(String emailDomain);
    boolean existsByEmailDomain(String emailDomain);
}
