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
@Table(name = "TeamMember", uniqueConstraints = {
        @UniqueConstraint(name = "UK_TEAM_USER", columnNames = {"teamId", "userId"})
})
public class TeamMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role;

    public void updateRole(TeamRole newRole) {
        this.role = newRole;
    }

    @Builder
    public TeamMember(Team team, User user, TeamRole role) {
        this.team = team;
        this.user = user;
        this.role = role;
    }
}
