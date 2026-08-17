package com._penLearning.Noddi.domain.user.entity;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationId", nullable = false)
    private Organization organization;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    /** 사용자 프로필에 표시할 자유 입력 부서명이다. 권한 판단에는 사용하지 않는다. */
    @Column(length = 20)
    private String department;

    /** 사용자 프로필에 표시할 자유 입력 직함이다. 권한 판단에는 사용하지 않는다. */
    @Column(length = 20)
    private String position;

    @Builder
    public User(
            Organization organization,
            String email,
            String name,
            String password,
            String department,
            String position
    ) {
        this.organization = organization;
        this.email = email;
        this.name = name;
        this.password = password;
        this.department = normalizeProfileValue(department);
        this.position = normalizeProfileValue(position);
    }

    public void updateProfile(String name, String department, String position) {
        this.name = name;
        if (department != null) {
            this.department = normalizeProfileValue(department);
        }
        if (position != null) {
            this.position = normalizeProfileValue(position);
        }
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    private static String normalizeProfileValue(String value) {
        return value == null ? null : value.strip();
    }
}
