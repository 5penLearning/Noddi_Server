package com._penLearning.Noddi.domain.qa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "QaAnswer")
public class QaAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionId", nullable = false, unique = true)
    private QaQuestion question;

    @Lob
    @Column(nullable = false)
    private String content;

    // 최신 수정본 ID (null이면 수정 없음)
    private Long latestRevisionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public QaAnswer(QaQuestion question, String content) {
        this.question = question;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public void updateLatestRevision(Long revisionId) {
        this.latestRevisionId = revisionId;
    }
}
