package com._penLearning.Noddi.domain.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class QaRequestDto {

    @Getter
    @NoArgsConstructor
    public static class CreateQuestion {
        @NotNull(message = "대상 팀 ID를 입력해주세요.")
        private Long targetTeamId;

        @NotBlank(message = "질문 내용을 입력해주세요.")
        @Size(max = 500, message = "질문 내용은 최대 500자까지 입력 가능합니다.")
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class CreateAnswer {
        @NotBlank(message = "답변 내용을 입력해주세요.")
        @Size(max = 1000, message = "답변 내용은 최대 1000자까지 입력 가능합니다.")
        private String content;
    }
}
