package com._penLearning.Noddi.domain.summary.dto;

import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.model.MeetingTranscriptSegment;
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
        private List<TranscriptSegmentDto> transcriptSegments;

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

            // 기존 데이터에는 JSON 값이 null일 수 있으므로 빈 목록으로 처리한다.
            List<MeetingTranscriptSegment> storedSegments =
                    meetingSummary.getTranscriptSegments();

            List<TranscriptSegmentDto> transcriptSegmentDtos =
                    storedSegments == null
                            ? List.of()
                            : storedSegments.stream()
                            .map(TranscriptSegmentDto::from)
                            .toList();

            return Detail.builder()
                    .meetingId(meeting.getMeetingId())
                    .summaryId(meetingSummary.getSummaryId())
                    .aiStatus(meeting.getAiStatus())
                    .summary(meetingSummary.getSummaryText())
                    .decisions(meetingSummary.getDecisions())
                    .issues(meetingSummary.getIssues())
                    .rawTranscript(formattedTranscript)
                    .transcriptSegments(transcriptSegmentDtos)
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
                    .transcriptSegments(List.of())
                    .actionItems(List.of())
                    .build();
        }
    }

    public record TranscriptSegmentDto(
            int sequence,
            String speakerLabel,
            long startTimeMs,
            long endTimeMs,
            String text
    ) {
        public static TranscriptSegmentDto from(
                MeetingTranscriptSegment segment
        ) {
            return new TranscriptSegmentDto(
                    segment.sequence(),
                    segment.speakerLabel(),
                    segment.startTimeMs(),
                    segment.endTimeMs(),
                    segment.text()
            );
        }
    }
}
