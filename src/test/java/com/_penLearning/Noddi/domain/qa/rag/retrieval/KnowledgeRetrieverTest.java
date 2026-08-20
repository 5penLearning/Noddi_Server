package com._penLearning.Noddi.domain.qa.rag.retrieval;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeDocumentFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrieverTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void searchesOnlyRequestedTeamAndMapsPineconeDocument() {
        KnowledgeRetriever retriever = new KnowledgeRetriever(vectorStore, 5, 0.3);
        Document pineconeDocument = Document.builder()
                .id("knowledge-transcript-20-0")
                .text("백엔드 서버의 최종 배포일은 8월 20일입니다.")
                .metadata(Map.of(
                        KnowledgeDocumentFactory.TEAM_ID, "10",
                        KnowledgeDocumentFactory.SOURCE_ID, "20",
                        KnowledgeDocumentFactory.SOURCE_TYPE, "TRANSCRIPT",
                        KnowledgeDocumentFactory.SOURCE_TITLE, "백엔드 배포 회의",
                        KnowledgeDocumentFactory.CHUNK_INDEX, 0.0
                ))
                .score(0.529)
                .build();

        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of(pineconeDocument));

        List<RetrievedKnowledge> results = retriever.retrieve(
                10L,
                "백엔드 서버는 언제 배포하나요?"
        );

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());

        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getQuery()).isEqualTo("백엔드 서버는 언제 배포하나요?");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.3);
        assertThat(request.getFilterExpression().toString())
                .contains("team_id")
                .contains("10");

        assertThat(results).containsExactly(new RetrievedKnowledge(
                "knowledge-transcript-20-0",
                20L,
                SourceType.TRANSCRIPT,
                "백엔드 배포 회의",
                "백엔드 서버의 최종 배포일은 8월 20일입니다.",
                0,
                0.529
        ));
    }

    @Test
    void returnsEmptyListWhenNoKnowledgeIsFound() {
        KnowledgeRetriever retriever = new KnowledgeRetriever(vectorStore, 5, 0.0);
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        List<RetrievedKnowledge> results = retriever.retrieve(10L, "근거가 없는 질문");

        assertThat(results).isEmpty();
    }
}
