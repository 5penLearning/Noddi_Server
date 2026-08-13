package com._penLearning.Noddi.domain.teamPage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TeamPageRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {

        @NotBlank
        @Size(max = 100)
        private String title;

        @NotBlank
        @Size(max = 15000)
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class Update {

        @NotBlank
        @Size(max = 100)
        private String title;

        @NotBlank
        @Size(max = 15000)
        private String content;
    }
}
