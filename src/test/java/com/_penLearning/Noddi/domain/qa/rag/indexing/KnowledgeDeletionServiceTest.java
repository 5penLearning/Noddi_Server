package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.QaKnowledgeIndex;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.repository.QaKnowledgeIndexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDeletionServiceTest {

    @Mock
    private QaKnowledgeIndexRepository knowledgeIndexRepository;

    @Mock
    private KnowledgeDocumentFactory documentFactory;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private QaKnowledgeIndex index;

    @Test
    void deletesPineconeChunksAndIndexStateForTeam() {
        KnowledgeDeletionService service = new KnowledgeDeletionService(
                knowledgeIndexRepository,
                documentFactory,
                vectorStore
        );
        List<QaKnowledgeIndex> indexes = List.of(index);
        List<String> documentIds = List.of("knowledge-transcript-10-0", "knowledge-transcript-10-1");

        when(knowledgeIndexRepository.findAllByTeam_TeamId(5L)).thenReturn(indexes);
        when(index.getSourceId()).thenReturn(10L);
        when(index.getSourceType()).thenReturn(SourceType.TRANSCRIPT);
        when(index.getChunkCount()).thenReturn(2);
        when(documentFactory.documentIds(10L, SourceType.TRANSCRIPT, 0, 2)).thenReturn(documentIds);

        service.deleteTeamKnowledge(5L);

        verify(vectorStore).delete(documentIds);
        verify(knowledgeIndexRepository).deleteAllInBatch(indexes);
    }
}
