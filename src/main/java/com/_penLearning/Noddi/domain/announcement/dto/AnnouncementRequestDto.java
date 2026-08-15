package com._penLearning.Noddi.domain.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AnnouncementRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create{

        @NotBlank
        @Size(max = 50)
        private String title;

        @NotBlank
        @Size(max = 1000)
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class Update{

        @NotBlank
        @Size(max = 50)
        private String title;

        @NotBlank
        @Size(max = 1000)
        private String content;
    }
}
