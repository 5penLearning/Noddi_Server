package com._penLearning.Noddi.domain.actionItem.dto;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ActionItemResponseDto {

    /** 내 팀 카드 한 개에 표시할 프로젝트·팀 정보와 개인 To-do 목록이다. */
    @Getter
    @Builder
    public static class TeamTodoGroup {
        private Long projectId;
        private String projectName;
        private Long teamId;
        private String teamName;
        private int todoCount;
        private List<Info> actionItems;

        public static TeamTodoGroup of(
                Team team,
                List<ActionItem> actionItems
        ) {
            List<Info> items = actionItems.stream()
                    .map(Info::from)
                    .toList();

            return TeamTodoGroup.builder()
                    .projectId(team.getProject().getProjectId())
                    .projectName(team.getProject().getName())
                    .teamId(team.getTeamId())
                    .teamName(team.getName())
                    .todoCount(items.size())
                    .actionItems(items)
                    .build();
        }
    }

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
