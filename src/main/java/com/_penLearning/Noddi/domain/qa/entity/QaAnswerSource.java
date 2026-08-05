package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "QaAnswerSource")
public class QaAnswerSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerId", nullable = false)
    private QaAnswer answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingId")
    private Meeting meeting;

    private String excerpt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public QaAnswerSource(QaAnswer answer, SourceType sourceType, Meeting meeting, String excerpt) {
        this.answer = answer;
        this.sourceType = sourceType;
        this.meeting = meeting;
        this.excerpt = excerpt;
        this.createdAt = LocalDateTime.now();
    }
}
