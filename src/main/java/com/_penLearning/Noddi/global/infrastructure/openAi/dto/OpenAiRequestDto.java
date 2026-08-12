package com._penLearning.Noddi.global.infrastructure.openAi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class OpenAiRequestDto {

    public record TeamMember(
            Long userId,
            String name
    ) {
    }

    public record ChatCompletion(
            String model,
            List<Message> messages,
            double temperature,

            @JsonProperty("response_format")
            ResponseFormat responseFormat
    ) {
    }

    public record Message(
            String role,
            String content
    ) {
    }

    public record ResponseFormat(
            String type,

            @JsonProperty("json_schema")
            JsonSchema jsonSchema
    ) {
    }

    // OpenAI response가 정해진 JSON 구조를 따르도록 설정하는 스키마 껍데기
    public record JsonSchema(
            String name,
            boolean strict,
            Map<String, Object> schema
    ) {
    }
}
