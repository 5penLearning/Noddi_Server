package com._penLearning.Noddi.domain.summary.model;

public record MeetingTranscriptSegment(
        int sequence,
        String speakerLabel,
        long startTimeMs,
        long endTimeMs,
        String text
) {

}
