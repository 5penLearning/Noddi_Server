package com._penLearning.Noddi.domain.actionItem.dto;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ActionItemResponseDto {

    @Getter
    @Builder
    public static class Info {
        private Long actionItemId;
        private Long meetingId;
        private String meetingTitle;

        private String content;

        private Long assigneeUserId;
        private String assigneeName;

        private LocalDate dueDate;
        private ActionItemStatus status;
        private Boolean isUncertain;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Info from(ActionItem actionItem) {
            return Info.builder()
                    .actionItemId(actionItem.getActionItemId())
                    .meetingId(actionItem.getMeeting().getMeetingId())
                    .meetingTitle(actionItem.getMeeting().getTitle())
                    .content(actionItem.getContent())
                    .assigneeUserId(
                            actionItem.getAssignee() == null
                                    ? null
                                    : actionItem.getAssignee().getUserId()
                    )
                    .assigneeName(
                            actionItem.getAssignee() == null
                                    ? null
                                    : actionItem.getAssignee().getName()
                    )
                    .dueDate(actionItem.getDueDate())
                    .status(actionItem.getStatus())
                    .isUncertain(actionItem.getIsUncertain())
                    .createdAt(actionItem.getCreatedAt())
                    .updatedAt(actionItem.getUpdatedAt())
                    .build();
        }
    }
}
