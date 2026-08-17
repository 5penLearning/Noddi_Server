package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeIndexServiceTest {

    @Mock
    private QaKnowledgeIndexStateService indexStateService;
    @Mock
    private KnowledgeDocumentFactory documentFactory;
    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private KnowledgeIndexService indexService;

    @Test
    void skipsEmbeddingWhenSourceContentHasNotChanged() throws Exception {
        KnowledgeSourceContent source = source("same content");
        when(indexStateService.get(20L, SourceType.TEAM_TEXT))
                .thenReturn(Optional.of(new KnowledgeIndexState(sha256("same content"), 1)));

        KnowledgeIndexResult result = indexService.synchronize(source);

        assertThat(result.skipped()).isTrue();
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void indexesChangedContentAndRecordsStateAfterVectorStoreWrite() {
        KnowledgeSourceContent source = source("new content");
        List<Document> documents = List.of(
                Document.builder().id("doc-1").text("chunk 1").build(),
                Document.builder().id("doc-2").text("chunk 2").build()
        );
        when(indexStateService.get(20L, SourceType.TEAM_TEXT)).thenReturn(Optional.empty());
        when(documentFactory.create(source)).thenReturn(documents);

        KnowledgeIndexResult result = indexService.synchronize(source);

        assertThat(result.indexedChunkCount()).isEqualTo(2);
        verify(vectorStore).add(documents);
        verify(indexStateService).recordSuccess(
                eq(20L),
                eq(10L),
                eq(SourceType.TEAM_TEXT),
                anyString(),
                eq(2)
        );
    }

    @Test
    void deletesChunksThatRemainAfterContentGetsShorter() {
        KnowledgeSourceContent source = source("shortened content");
        List<Document> documents = List.of(Document.builder().id("doc-1").text("chunk 1").build());
        List<String> staleIds = List.of("knowledge-team_text-20-1", "knowledge-team_text-20-2");
        when(indexStateService.get(20L, SourceType.TEAM_TEXT))
                .thenReturn(Optional.of(new KnowledgeIndexState("old-hash", 3)));
        when(documentFactory.create(source)).thenReturn(documents);
        when(documentFactory.documentIds(20L, SourceType.TEAM_TEXT, 1, 3)).thenReturn(staleIds);

        indexService.synchronize(source);

        verify(vectorStore).delete(staleIds);
    }

    private KnowledgeSourceContent source(String content) {
        return new KnowledgeSourceContent(20L, 10L, SourceType.TEAM_TEXT, "team page", content);
    }

    private String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
