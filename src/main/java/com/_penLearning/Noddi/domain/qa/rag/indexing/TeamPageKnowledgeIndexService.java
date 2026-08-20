package com._penLearning.Noddi.domain.qa.rag.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 공유페이지 원문을 읽어 공통 Pinecone 인덱싱 흐름에 전달한다. */
@Service
@RequiredArgsConstructor
public class TeamPageKnowledgeIndexService {

    private final TeamPageKnowledgeSourceReader sourceReader;
    private final KnowledgeIndexService knowledgeIndexService;

    public KnowledgeIndexResult index(Long pageId) {
        return knowledgeIndexService.synchronize(sourceReader.read(pageId));
    }
}
