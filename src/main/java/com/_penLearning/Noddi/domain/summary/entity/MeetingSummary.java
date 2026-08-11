package com._penLearning.Noddi.domain.summary.entity;

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
@Table(name = "MeetingSummary")
public class MeetingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingId", nullable = false, unique = true)
    private Meeting meeting;

    @Lob
    @Column(nullable = false)
    private String summaryText;

    @Lob
    @Column(nullable = false)
    private String rawTranscript;

    // List<String>을 JSON 직렬화하여 저장
    private String decisions;

    private String issues;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public MeetingSummary(Meeting meeting, String summaryText, String decisions, String issues, String rawTranscript) {
        this.meeting = meeting;
        this.summaryText = summaryText;
        this.decisions = decisions;
        this.issues = issues;
        this.createdAt = LocalDateTime.now();
        this.rawTranscript = rawTranscript;
    }
}
