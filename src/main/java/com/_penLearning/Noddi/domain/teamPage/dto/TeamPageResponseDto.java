package com._penLearning.Noddi.domain.teamPage.dto;

import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class TeamPageResponseDto {

    @Getter
    @Builder
    public static class Result {

        private Long pageId;

        public static Result from(TeamPage teamPage) {
            return Result.builder()
                    .pageId(teamPage.getPageId())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Summary {

        private Long pageId;
        private String title;
        private Long authorId;
        private String authorName;
        private LocalDateTime updatedAt;

        public static Summary from(TeamPage teamPage) {
            return Summary.builder()
                    .pageId(teamPage.getPageId())
                    .title(teamPage.getTitle())
                    .authorId(teamPage.getAuthor().getUserId())
                    .authorName(teamPage.getAuthor().getName())
                    .updatedAt(teamPage.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Detail{
        private Long pageId;
        private Long teamId;
        private Long authorId;
        private String authorName;
        private String title;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Detail from(TeamPage teamPage) {
            return Detail.builder()
                    .pageId(teamPage.getPageId())
                    .teamId(teamPage.getTeam().getTeamId())
                    .authorId(teamPage.getAuthor().getUserId())
                    .authorName(teamPage.getAuthor().getName())
                    .title(teamPage.getTitle())
                    .content(teamPage.getContent())
                    .createdAt(teamPage.getCreatedAt())
                    .updatedAt(teamPage.getUpdatedAt())
                    .build();
        }
    }
}
