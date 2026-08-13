package com._penLearning.Noddi.domain.summary.entity;

import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawTranscript;

    // MySQL JSON배열로 저장
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON",nullable = false)
    private List<String> decisions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON",nullable = false)
    private List<String> issues;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public MeetingSummary(Meeting meeting, String summaryText, List<String> decisions, List<String> issues, String rawTranscript) {
        this.meeting = meeting;
        this.summaryText = summaryText;
        // OpenAI가 null을 반환하더라도 DB에는 빈 JSON 배열을 저장한다.
        this.decisions = decisions != null
                ? new ArrayList<>(decisions)
                : new ArrayList<>();

        this.issues = issues != null
                ? new ArrayList<>(issues)
                : new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.rawTranscript = rawTranscript;
    }
}
