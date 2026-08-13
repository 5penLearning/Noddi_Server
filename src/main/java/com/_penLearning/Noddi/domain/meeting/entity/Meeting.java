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
    private LocalDateTime aiProcessingStartedAt;

    //webRTC 방 ID LAZY 방식으로 주입(회의 시작 버튼을 눌러야 회의가 열림)
    private String roomName;
    private String recordingId;

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
     * Daily.co 녹음 ID 업데이트
     */
    public void updateRecordingId(String recordingId) {
        this.recordingId = recordingId;
    }

    public boolean tryStartAiProcessing() {
        if (this.status != MeetingStatus.ENDED) {
            return false;
        }
        if (this.recordingId == null) {
            return false;
        }
        if (this.aiStatus != AiStatus.PENDING) {
            return false;
        }
        //회의 종료와 녹음 준비가 모두 완료되었고 PENDING 상태일 때만 변경
        this.aiStatus = AiStatus.PROCESSING;
        this.aiProcessingStartedAt = LocalDateTime.now();
        return true;
    }

    //Ai 요약 관련 메소드
    public void retryAiProcessing() {
        if (this.status != MeetingStatus.ENDED) {
            throw new GeneralException(MeetingErrorCode.AI_RETRY_NOT_ALLOWED);
        }

        if (this.recordingId == null) {
            throw new GeneralException(MeetingErrorCode.RECORDING_NOT_READY);
        }

        if (this.aiStatus != AiStatus.FAILED) {
            throw new GeneralException(MeetingErrorCode.AI_RETRY_NOT_ALLOWED);
        }
        this.aiStatus = AiStatus.PROCESSING;
        this.aiProcessingStartedAt = LocalDateTime.now();
    }

    public void completeAiProcessing() {
        this.aiStatus = AiStatus.COMPLETED;
    }

    public void failAiProcessing() {
        this.aiStatus = AiStatus.FAILED;
    }
}
