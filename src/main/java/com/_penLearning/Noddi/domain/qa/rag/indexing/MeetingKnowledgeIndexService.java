package com._penLearning.Noddi.domain.qa.rag.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 회의 전사를 공통 지식 인덱싱 흐름에 전달한다. */
@Service
@RequiredArgsConstructor
public class MeetingKnowledgeIndexService {

    private final MeetingKnowledgeSourceReader sourceReader;
    private final KnowledgeIndexService knowledgeIndexService;

    public MeetingKnowledgeIndexResult index(Long meetingId) {
        KnowledgeSourceContent source = sourceReader.read(meetingId);
        KnowledgeIndexResult result = knowledgeIndexService.synchronize(source);

        return new MeetingKnowledgeIndexResult(
                meetingId,
                result.indexedChunkCount(),
                result.skipped() ? 1 : 0
        );
    }
}
