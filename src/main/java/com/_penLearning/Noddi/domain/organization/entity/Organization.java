package com._penLearning.Noddi.domain.organization.entity;

import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Organization")
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String emailDomain;

    @Builder
    public Organization(String name, String emailDomain) {
        this.name = name;
        this.emailDomain = emailDomain;
    }
}
