package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentFactoryTest {

    private final KnowledgeDocumentFactory documentFactory = new KnowledgeDocumentFactory();

    @Test
    void createsDocumentWithTenantAndCommonSourceMetadata() {
        KnowledgeSourceContent source = new KnowledgeSourceContent(
                20L,
                10L,
                SourceType.TRANSCRIPT,
                "weekly meeting",
                "transcript content"
        );

        List<Document> documents = documentFactory.create(source);

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getId()).isEqualTo("knowledge-transcript-20-0");
        assertThat(document.getMetadata())
                .containsEntry(KnowledgeDocumentFactory.TEAM_ID, "10")
                .containsEntry(KnowledgeDocumentFactory.SOURCE_ID, "20")
                .containsEntry(KnowledgeDocumentFactory.SOURCE_TYPE, "TRANSCRIPT")
                .containsEntry(KnowledgeDocumentFactory.SOURCE_TITLE, "weekly meeting")
                .containsEntry(KnowledgeDocumentFactory.CHUNK_INDEX, 0);
    }

    @Test
    void generatesDeterministicDocumentIds() {
        assertThat(documentFactory.documentIds(20L, SourceType.TRANSCRIPT, 1, 4))
                .containsExactly(
                        "knowledge-transcript-20-1",
                        "knowledge-transcript-20-2",
                        "knowledge-transcript-20-3"
                );
    }
}
