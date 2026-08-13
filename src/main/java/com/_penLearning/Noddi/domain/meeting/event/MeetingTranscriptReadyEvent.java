package com._penLearning.Noddi.domain.meeting.event;

/** 회의 전사 원문과 요약이 DB에 저장되어 RAG 색인을 시작할 수 있음을 알리는 이벤트다. */
public record MeetingTranscriptReadyEvent(Long meetingId) {
}
