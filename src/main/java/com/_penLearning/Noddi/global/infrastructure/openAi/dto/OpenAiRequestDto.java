package com._penLearning.Noddi.global.infrastructure.openAi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class OpenAiRequestDto {

    public record TeamMemter(
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

    //reponse를 정해진 구조로 강제
    public record JsonSchema(
            String name,
            boolean strict,
            Map<String, Object> schema
    ) {
    }
}
