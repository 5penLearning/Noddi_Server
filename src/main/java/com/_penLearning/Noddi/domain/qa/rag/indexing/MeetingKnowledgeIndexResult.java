package com._penLearning.Noddi.domain.qa.rag.indexing;

public record MeetingKnowledgeIndexResult(
        Long meetingId,
        int indexedChunkCount,
        int skippedSourceCount
) {
}
