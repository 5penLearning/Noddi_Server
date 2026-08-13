package com._penLearning.Noddi.domain.teamPage.dto;

import lombok.Builder;
import lombok.Getter;

public class TeamPageResponseDto {

    @Getter
    @Builder
    public static class Info{
        private Long authorId;
        private String title;
        private String content;
    }
}
