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
class MeetingKnowledgeIndexServiceTest {

    @Mock
    private MeetingKnowledgeSourceReader sourceReader;
    @Mock
    private QaKnowledgeIndexStateService indexStateService;
    @Mock
    private KnowledgeDocumentFactory documentFactory;
    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private MeetingKnowledgeIndexService indexService;

    @Test
    void skipsEmbeddingWhenSourceContentHasNotChanged() throws Exception {
        KnowledgeSourceContent source = source("same transcript");
        when(sourceReader.read(20L)).thenReturn(source);
        when(indexStateService.get(20L, SourceType.TRANSCRIPT))
                .thenReturn(Optional.of(new KnowledgeIndexState(sha256("same transcript"), 1)));

        MeetingKnowledgeIndexResult result = indexService.index(20L);

        assertThat(result.indexedChunkCount()).isZero();
        assertThat(result.skippedSourceCount()).isEqualTo(1);
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void indexesChangedContentAndRecordsStateAfterVectorStoreWrite() {
        KnowledgeSourceContent source = source("new transcript");
        List<Document> documents = List.of(
                Document.builder().id("doc-1").text("chunk 1").build(),
                Document.builder().id("doc-2").text("chunk 2").build()
        );
        when(sourceReader.read(20L)).thenReturn(source);
        when(indexStateService.get(20L, SourceType.TRANSCRIPT)).thenReturn(Optional.empty());
        when(documentFactory.create(source)).thenReturn(documents);

        MeetingKnowledgeIndexResult result = indexService.index(20L);

        assertThat(result.indexedChunkCount()).isEqualTo(2);
        assertThat(result.skippedSourceCount()).isZero();
        verify(vectorStore).add(documents);
        verify(indexStateService).recordSuccess(
                eq(20L),
                eq(10L),
                eq(SourceType.TRANSCRIPT),
                anyString(),
                eq(2)
        );
    }

    private KnowledgeSourceContent source(String transcript) {
        return new KnowledgeSourceContent(
                20L,
                10L,
                SourceType.TRANSCRIPT,
                "weekly meeting",
                transcript
        );
    }

    private String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
