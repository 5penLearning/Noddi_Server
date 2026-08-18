package com._penLearning.Noddi.domain.summary.dto;

import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class SummaryResponseDto {

    @Getter
    @Builder
    public static class Detail {
        private Long meetingId;
        private Long summaryId;
        private AiStatus aiStatus;

        private String summary;
        private List<String> decisions;
        private List<String> issues;
        private String rawTranscript;

        private List<ActionItemResponseDto.Info> actionItems;

        /**
         * AI 회의록 생성이 완료된 경우 사용하는 응답
         */
        public static Detail completed(
                MeetingSummary meetingSummary,
                List<ActionItem> actionItems,
                String formattedTranscript
        ) {
            Meeting meeting = meetingSummary.getMeeting();

            return Detail.builder()
                    .meetingId(meeting.getMeetingId())
                    .summaryId(meetingSummary.getSummaryId())
                    .aiStatus(meeting.getAiStatus())
                    .summary(meetingSummary.getSummaryText())
                    .decisions(meetingSummary.getDecisions())
                    .issues(meetingSummary.getIssues())
                    .rawTranscript(formattedTranscript)
                    .actionItems(
                            actionItems.stream()
                                    .map(ActionItemResponseDto.Info::from)
                                    .toList()
                    )
                    .build();
        }

        /**
         * AI 처리가 아직 끝나지 않았거나 실패한 경우 사용하는 응답
         * 프론트는 aiStatus를 보고 로딩 또는 재시도 화면을 표시
         */
        public static Detail withoutResult(Meeting meeting) {
            return Detail.builder()
                    .meetingId(meeting.getMeetingId())
                    .summaryId(null)
                    .aiStatus(meeting.getAiStatus())
                    .summary(null)
                    .decisions(List.of())
                    .issues(List.of())
                    .rawTranscript(null)
                    .actionItems(List.of())
                    .build();
        }
    }
}
