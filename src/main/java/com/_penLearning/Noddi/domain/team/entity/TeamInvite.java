package com._penLearning.Noddi.domain.team.entity;

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
@Table(name = "TeamInvite")
public class TeamInvite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inviteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviterId", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviteeId", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status;

    // BaseEntity의 createdAt 필드를 활용 (필드명은 실제 환경에 맞게 수정)
    public boolean isExpired(int validDays) {
        return this.getCreatedAt().plusDays(validDays).isBefore(LocalDateTime.now());
    }

    // 만료 상태 변경 메서드 추가
    public void expire() {
        this.status = InviteStatus.EXPIRED;
    }

    private LocalDateTime respondedAt;

    @Builder
    public TeamInvite(Team team, User inviter, User invitee) {
        this.team = team;
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
}
