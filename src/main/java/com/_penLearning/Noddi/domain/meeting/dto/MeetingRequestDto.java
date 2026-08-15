package com._penLearning.Noddi.domain.meeting.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MeetingRequestDto {
    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotNull(message = "팀 ID는 필수입니다.")
        private Long teamId;

        @NotBlank(message = "회의 제목은 필수입니다.")
        private String title;

        @Size(max = 200, message = "회의 안건은 200자 이하여야 합니다.")
        private String agenda;

        private LocalDateTime scheduledStartAt;

        private LocalDateTime scheduledEndAt;


        @JsonIgnore
        @AssertTrue(message = "예약 시작 시간과 종료 시간은 함께 입력해야 합니다.")
        public boolean isSchedulePairValid() {
            return (scheduledStartAt == null) == (scheduledEndAt == null);
        }

        @JsonIgnore
        @AssertTrue(message = "예약 종료 시간은 시작 시간보다 뒤여야 합니다.")
        public boolean isScheduleTimeValid() {
            if (scheduledStartAt == null || scheduledEndAt == null) {
                return true;
            }
            return scheduledEndAt.isAfter(scheduledStartAt);
        }
    }
}
