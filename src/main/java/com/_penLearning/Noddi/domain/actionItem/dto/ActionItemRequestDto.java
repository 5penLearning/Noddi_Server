package com._penLearning.Noddi.domain.actionItem.dto;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class ActionItemRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {
        @NotBlank(message = "할 일 내용은 필수입니다.")
        private String content;
        private Long assigneeUserId;
        private LocalDate dueDate;
    }

    @Getter
    @NoArgsConstructor
    public static class Update {
        @NotBlank(message = "할 일 내용은 필수입니다.")
        private String content;
        // 담당자가 정해지지 않은 할 수도 있으므로 nullable
        private Long assigneeUserId;
        private LocalDate dueDate;
        @NotNull(message = "할 일 상태는 필수입니다.")
        private ActionItemStatus status;
    }
}
