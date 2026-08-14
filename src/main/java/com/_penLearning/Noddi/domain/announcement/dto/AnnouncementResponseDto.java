package com._penLearning.Noddi.domain.announcement.dto;

import com._penLearning.Noddi.domain.announcement.entity.Announcement;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AnnouncementResponseDto {

    @Getter
    @Builder
    public static class Result {
        private Long announcementId;

        public static Result from(Announcement announcement) {
            return Result.builder()
                    .announcementId(announcement.getAnnouncementId())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Summary {
        private Long announcementId;
        private Long teamId;
        private String teamName;
        private String title;
        private String content;
        private LocalDateTime updatedAt;

        public static Summary from(Announcement announcement) {
            return Summary.builder()
                    .announcementId(announcement.getAnnouncementId())
                    .teamId(announcement.getTeam().getTeamId())
                    .teamName(announcement.getTeam().getName())
                    .title(announcement.getTitle())
                    .content(announcement.getContent())
                    .updatedAt(announcement.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Detail {
        private Long announcementId;
        private Long teamId;
        private String teamName;
        private Long authorId;
        private String authorName;
        private String title;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Detail from(Announcement announcement) {
            return Detail.builder()
                    .announcementId(announcement.getAnnouncementId())
                    .teamId(announcement.getTeam().getTeamId())
                    .teamName(announcement.getTeam().getName())
                    .authorId(announcement.getAuthor().getUserId())
                    .authorName(announcement.getAuthor().getName())
                    .title(announcement.getTitle())
                    .content(announcement.getContent())
                    .createdAt(announcement.getCreatedAt())
                    .updatedAt(announcement.getUpdatedAt())
                    .build();
        }
    }
}
