package com._penLearning.Noddi.domain.qa.rag.indexing;

/** 하나의 지식 원본을 Pinecone과 동기화한 결과다. */
public record KnowledgeIndexResult(
        int indexedChunkCount,
        boolean skipped
) {

    public static KnowledgeIndexResult indexed(int chunkCount) {
        return new KnowledgeIndexResult(chunkCount, false);
    }

    public static KnowledgeIndexResult skippedResult() {
        return new KnowledgeIndexResult(0, true);
    }
}
