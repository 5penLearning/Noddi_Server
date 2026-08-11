package com._penLearning.Noddi.domain.organization.dto;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import lombok.Builder;
import lombok.Getter;

public class OrganizationResponseDto {

    @Getter
    @Builder
    public static class Info {
        private Long organizationId;
        private String name;
        private String emailDomain;

        public static Info from(Organization organization) {
            return Info.builder()
                    .organizationId(organization.getOrganizationId())
                    .name(organization.getName())
                    .emailDomain(organization.getEmailDomain())
                    .build();
        }
    }
}
