package com._penLearning.Noddi.domain.meeting.entity;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.code.MeetingStatus;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import com._penLearning.Noddi.global.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiStatus aiStatus;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    //webRTC 방 ID LAZY 방식으로 주입(회의 시작 버튼을 눌러야 회의가 열림)
    private String roomName;
    private String recordingUrl;
    @Lob
    private String transcriptText;

    @Builder
    public Meeting(Team team, String title, User createdBy) {
        this.team = team;
        this.title = title;
        this.createdBy = createdBy;
        this.status = MeetingStatus.SCHEDULED;
        this.aiStatus = AiStatus.PENDING;
    }

    public void start(String roomName) {
        if (this.status != MeetingStatus.SCHEDULED) {
            throw new GeneralException(MeetingErrorCode.INVALID_STATUS_FOR_START);
        }
        this.status = MeetingStatus.IN_PROGRESS;
        this.roomName = roomName;
        this.startedAt = LocalDateTime.now();
    }

    public void end() {
        if (this.status != MeetingStatus.IN_PROGRESS) {
            throw new GeneralException(MeetingErrorCode.INVALID_STATUS_FOR_END);
        }
        this.status = MeetingStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * 3. 녹음본 URL 업데이트
     * - 시점: WebRTC Webhook(room.empty 등)으로 S3 업로드 완료 알림 수신 시
     */
    public void updateRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    //Ai 요약 관련 메소드
    public void startAiProcessing() {
        if (this.status != MeetingStatus.ENDED) {
            throw new GeneralException(MeetingErrorCode.INVALID_STATUS_FOR_SUMMARY);
        }
        if (this.recordingUrl == null) {
            throw new GeneralException(MeetingErrorCode.RECORDING_NOT_READY);
        }
        this.aiStatus = AiStatus.PROCESSING;
    }
    public void completeAiProcessing(String transcriptText) {
        this.aiStatus = AiStatus.COMPLETED;
        this.transcriptText = transcriptText;
    }

    public void failAiProcessing() {
        this.aiStatus = AiStatus.FAILED;
    }
}
