package com._penLearning.Noddi.domain.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MeetingRequestDto {
    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotNull(message = "팀 ID는 필수입니다.")
        private Long teamId;

        @NotBlank(message = "회의 제목은 필수입니다.")
        private String title;
    }
}
