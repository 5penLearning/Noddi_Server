package com._penLearning.Noddi.domain.summary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class SummaryRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Update {
        @NotBlank(message = "회의 요약은 비어 있을 수 없습니다.")
        private String summary;
        @NotNull(message = "결정사항 목록은 필수입니다.")
        private List<@NotBlank String> decisions;
        @NotNull(message = "논의 이슈 목록은 필수입니다.")
        private List<@NotBlank String> issues;
    }
}
