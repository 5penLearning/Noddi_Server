package com._penLearning.Noddi.domain.qa.rag.retrieval;

import com._penLearning.Noddi.domain.qa.entity.SourceType;

/**
 * 벡터 검색을 통해 찾은 하나의 근거 청크다.
 * Pinecone의 Document를 외부 계층에 직접 노출하지 않고 프롬프트와 출처 저장에 필요한 값만 전달한다.
 */
public record RetrievedKnowledge(
        String documentId,
        Long sourceId,
        SourceType sourceType,
        String sourceTitle,
        String content,
        int chunkIndex,
        double score
) {
}
