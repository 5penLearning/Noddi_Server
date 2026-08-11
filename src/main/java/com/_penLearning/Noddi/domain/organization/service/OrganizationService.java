package com._penLearning.Noddi.domain.organization.service;

import com._penLearning.Noddi.domain.organization.code.OrganizationErrorCode;
import com._penLearning.Noddi.domain.organization.dto.OrganizationResponseDto;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationResponseDto.Info getOrganization(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        return OrganizationResponseDto.Info.from(organization);
    }

    public List<OrganizationResponseDto.Info> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(OrganizationResponseDto.Info::from)
                .toList();
    }
}
