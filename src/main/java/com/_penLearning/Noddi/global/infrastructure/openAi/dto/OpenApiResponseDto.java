package com._penLearning.Noddi.global.infrastructure.openAi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

public class OpenApiResponseDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transcription(
            String text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletion(
            List<Choice> choices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            Message message
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String content, String refusal
    ) {
    }


    public record MeetingSummary(
            String summary,
            List<String> decisions,
            List<String> issues,
            List<Task> tasks
    ) {
    }

    public record Task(
            String content,
            Long assigneeUserId,
            String assigneeName,
            LocalDate dueDate,
            boolean isUncertain
    ) {
    }

}
