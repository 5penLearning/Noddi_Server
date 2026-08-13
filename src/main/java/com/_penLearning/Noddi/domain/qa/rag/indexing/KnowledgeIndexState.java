package com._penLearning.Noddi.domain.qa.rag.indexing;

public record KnowledgeIndexState(
        String contentHash,
        int chunkCount
) {
}
