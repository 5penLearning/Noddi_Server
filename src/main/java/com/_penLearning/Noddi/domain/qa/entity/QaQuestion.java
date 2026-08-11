package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.team.entity.Team;
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
@Table(name = "QaQuestion")
public class QaQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionerId", nullable = false)
    private User questioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "targetTeamId", nullable = false)
    private Team targetTeam;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QaStatus status;

    @Builder
    public QaQuestion(User questioner, Team targetTeam, String content) {
        this.questioner = questioner;
        this.targetTeam = targetTeam;
        this.content = content;
        this.status = QaStatus.PENDING;
    }

    public void markAsAnswered() {
        this.status = QaStatus.ANSWERED;
    }
}
