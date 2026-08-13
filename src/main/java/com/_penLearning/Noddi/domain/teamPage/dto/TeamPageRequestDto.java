package com._penLearning.Noddi.domain.teamPage.dto;

import lombok.Builder;
import lombok.Getter;

public class TeamPageRequestDto {

    @Getter
    @Builder
    public static class create {
        private String title;
        private String content;
    }

    @Getter
    @Builder
    public static class update{
        private String title;
        private String content;
    }
}
