package com._penLearning.Noddi.domain.notification.dto;

import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NotificationRequestDto {

    /**
     * 묶음 알림의 '자세히보기'를 눌렀을 때 사용
     *
     * 현재는 같은 프로젝트·팀에 속한
     * QA_AI_REVIEW_REQUIRED 알림 묶음을 식별한다.
     */
    @Getter
    @NoArgsConstructor
    public static class ReadGroup {

        @NotNull(message = "프로젝트 ID는 필수입니다.")
        private Long projectId;

        @NotNull(message = "팀 ID는 필수입니다.")
        private Long teamId;

        @NotNull(message = "알림 타입은 필수입니다.")
        @Schema(
                description = "묶음 처리할 알림 타입. 현재 AI 답변 검토 요청만 지원합니다.",
                example = "QA_AI_REVIEW_REQUIRED",
                allowableValues = {"QA_AI_REVIEW_REQUIRED"}
        )
        private NotificationType type;
    }

    /**
     * 묶음 알림의 X 버튼을 눌렀을 때 사용한다.
     *
     * ALL 조회에서는 같은 프로젝트·팀의 검토 알림이
     * 읽은 묶음과 안 읽은 묶음으로 분리되므로
     * 사용자가 실제로 누른 묶음의 read 상태도 전달받는다.
     */
    @Getter
    @NoArgsConstructor
    public static class HideGroup {

        @NotNull(message = "프로젝트 ID는 필수입니다.")
        private Long projectId;

        @NotNull(message = "팀 ID는 필수입니다.")
        private Long teamId;

        @NotNull(message = "알림 타입은 필수입니다.")
        @Schema(
                description = "묶음 처리할 알림 타입. 현재 AI 답변 검토 요청만 지원합니다.",
                example = "QA_AI_REVIEW_REQUIRED",
                allowableValues = {"QA_AI_REVIEW_REQUIRED"}
        )
        private NotificationType type;

        /*
         * Boolean을 사용하는 이유:
         * primitive boolean이면 요청에서 read를 생략해도 false가 되어
         * 누락과 명시적인 false를 구분할 수 없다.
         */
        @NotNull(message = "읽음 상태는 필수입니다.")
        private Boolean read;
    }
}
