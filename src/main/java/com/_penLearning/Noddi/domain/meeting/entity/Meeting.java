package com._penLearning.Noddi.domain.meeting.entity;

import com._penLearning.Noddi.domain.team.entity.Team;
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
@Table(name = "Meeting")
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long meetingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String roomId;
    private String recordingUrl;

    @Lob
    private String transcriptText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiStatus aiStatus;

    @Builder
    public Meeting(Team team, String title, User createdBy) {
        this.team = team;
        this.title = title;
        this.createdBy = createdBy;
        this.status = MeetingStatus.SCHEDULED;
        this.aiStatus = AiStatus.PENDING;
    }

    public void start(String roomId) {
        this.status = MeetingStatus.IN_PROGRESS;
        this.roomId = roomId;
        this.startedAt = LocalDateTime.now();
    }

    public void end(String recordingUrl, String transcriptText) {
        this.status = MeetingStatus.ENDED;
        this.endedAt = LocalDateTime.now();
        this.recordingUrl = recordingUrl;
        this.transcriptText = transcriptText;
        this.aiStatus = AiStatus.PROCESSING;
    }

    public void completeAi() {
        this.aiStatus = AiStatus.COMPLETED;
    }

    public void failAi() {
        this.aiStatus = AiStatus.FAILED;
    }
}
