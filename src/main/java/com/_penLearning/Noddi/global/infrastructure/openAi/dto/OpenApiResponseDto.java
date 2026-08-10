package com._penLearning.Noddi.global.infrastructure.openAi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class OpenApiResponseDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transcription(String text) {
    }
}
