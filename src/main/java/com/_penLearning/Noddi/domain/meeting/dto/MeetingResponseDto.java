package com._penLearning.Noddi.domain.meeting.dto;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.code.MeetingStatus;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.entity.MeetingParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class MeetingResponseDto {
    private static final String DAILY_URL_PREFIX = "https://noddi.daily.co/";

    @Getter
    @Builder
    public static class Info {
        private Long meetingId;
        private Long teamId;
        private String title;
        private MeetingStatus status;
        private AiStatus aiStatus;
        private String roomName;
        private String roomUrl; // 조합된 프론트엔드 접속 URL
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private LocalDateTime createdAt;
        public static Info from(Meeting meeting) {
            String url = meeting.getRoomName() != null
                    ? DAILY_URL_PREFIX + meeting.getRoomName()
                    : null;
            return Info.builder()
                    .meetingId(meeting.getMeetingId())
                    .teamId(meeting.getTeam().getTeamId())
                    .title(meeting.getTitle())
                    .status(meeting.getStatus())
                    .aiStatus(meeting.getAiStatus())
                    .roomName(meeting.getRoomName())
                    .roomUrl(url)
                    .startedAt(meeting.getStartedAt())
                    .endedAt(meeting.getEndedAt())
                    .createdAt(meeting.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Start {
        private Long meetingId;
        private String roomName;
        private String roomUrl;

        public static Start from(Meeting meeting) {
            String url = meeting.getRoomName() != null
                    ? DAILY_URL_PREFIX + meeting.getRoomName()
                    : null;
            return Start.builder()
                    .meetingId(meeting.getMeetingId())
                    .roomName(meeting.getRoomName())
                    .roomUrl(url)
                    .build();
        }
    }


    @Getter
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String name;
        private String email;
        private LocalDateTime joinedAt;
        public static ParticipantInfo from(MeetingParticipant participant) {
            return ParticipantInfo.builder()
                    .userId(participant.getUser().getUserId())
                    .name(participant.getUser().getName())
                    .email(participant.getUser().getEmail())
                    .joinedAt(participant.getJoinedAt())
                    .build();
        }
    }
}
