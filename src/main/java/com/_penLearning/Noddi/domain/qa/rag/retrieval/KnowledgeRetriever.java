package com._penLearning.Noddi.domain.qa.rag.retrieval;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeDocumentFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * 질문과 의미가 가까운 팀 자료를 Pinecone에서 검색한다.
 * 모든 검색에 team_id 필터를 강제로 적용하여 다른 팀 자료가 후보에 포함되는 것을 막는다.
 */
@Component
public class KnowledgeRetriever {

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public KnowledgeRetriever(
            VectorStore vectorStore,
            @Value("${qa.rag.top-k:5}") int topK, // 유사도 높은 청크를 몇 개 가져올 것인지
            @Value("${qa.rag.similarity-threshold:0.2}") double similarityThreshold // 유사도 점수 커트라인
    ) {
        Assert.isTrue(topK > 0, "qa.rag.top-k must be greater than 0");
        Assert.isTrue(
                similarityThreshold >= 0.0 && similarityThreshold <= 1.0,
                "qa.rag.similarity-threshold must be between 0.0 and 1.0"
        );
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<RetrievedKnowledge> retrieve(Long teamId, String question) {
        Assert.notNull(teamId, "teamId must not be null");
        Assert.hasText(question, "question must not be blank");

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                // team_id는 색인할 때 문자열로 저장하므로 검색 필터도 문자열로 비교한다.
                .filterExpression("%s == '%s'".formatted(
                        KnowledgeDocumentFactory.TEAM_ID,
                        teamId
                ))
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(this::toRetrievedKnowledge)
                .toList();
    }

    private RetrievedKnowledge toRetrievedKnowledge(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return new RetrievedKnowledge(
                document.getId(),
                toLong(requiredMetadata(metadata, KnowledgeDocumentFactory.SOURCE_ID)),
                SourceType.valueOf(requiredMetadata(metadata, KnowledgeDocumentFactory.SOURCE_TYPE).toString()),
                requiredMetadata(metadata, KnowledgeDocumentFactory.SOURCE_TITLE).toString(),
                document.getText(),
                toInt(requiredMetadata(metadata, KnowledgeDocumentFactory.CHUNK_INDEX)),
                document.getScore() != null ? document.getScore() : 0.0
        );
    }

    private Object requiredMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            throw new IllegalStateException("Required knowledge metadata is missing: " + key);
        }
        return value;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
