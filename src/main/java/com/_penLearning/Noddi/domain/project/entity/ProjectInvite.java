package com._penLearning.Noddi.domain.project.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ProjectInvite")
public class ProjectInvite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inviteId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projectId", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviterId", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviteeId", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status;

    private LocalDateTime respondedAt;

    @Builder
    public ProjectInvite(Project project, User inviter, User invitee) {
        this.project = project;
        this.inviter = inviter;
        this.invitee = invitee;
        this.status = InviteStatus.PENDING;
    }

    public void accept() {
        this.status = InviteStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = InviteStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public boolean isExpired(int validDays) {
        // BaseEntity의 createdAt을 활용
        return this.getCreatedAt().plusDays(validDays).isBefore(LocalDateTime.now());
    }

    public void expire() {
        this.status = InviteStatus.EXPIRED;
    }
}
