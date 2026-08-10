package com._penLearning.Noddi.domain.project.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ProjectMember", uniqueConstraints = {
@UniqueConstraint(name = "UK_PROJECT_USER", columnNames = {"projectId", "userId"})
})
public class ProjectMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectId", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinStatus status; // 상태 필드 추가

    @Builder
    public ProjectMember(Project project, User user, ProjectRole role, JoinStatus status) {
        this.project = project;
        this.user = user;
        this.role = role;
        this.status = status;
    }

    // 객체 생성 책임을 캡슐화한 정적 팩토리 메서드
    public static ProjectMember create(Project project, User user, ProjectRole role, JoinStatus status) {
        return ProjectMember.builder()
                .project(project)
                .user(user)
                .role(role)
                .status(status)
                .build();
    }

    // 상태 변경 로직 캡슐화
    public void acceptInvitation() {
        this.status = JoinStatus.JOINED;
    }

    public void rejectInvitation() {
        this.status = JoinStatus.REJECTED;
    }

    public void updateRole(ProjectRole newRole) {
        this.role = newRole;
    }
}
