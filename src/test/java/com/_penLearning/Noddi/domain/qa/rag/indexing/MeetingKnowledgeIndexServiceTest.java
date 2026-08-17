package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingKnowledgeIndexServiceTest {

    @Mock
    private MeetingKnowledgeSourceReader sourceReader;
    @Mock
    private KnowledgeIndexService knowledgeIndexService;

    @InjectMocks
    private MeetingKnowledgeIndexService indexService;

    @Test
    void delegatesTranscriptToCommonKnowledgeIndexer() {
        KnowledgeSourceContent source = new KnowledgeSourceContent(
                20L, 10L, SourceType.TRANSCRIPT, "weekly meeting", "new transcript"
        );
        when(sourceReader.read(20L)).thenReturn(source);
        when(knowledgeIndexService.synchronize(source)).thenReturn(KnowledgeIndexResult.indexed(2));

        MeetingKnowledgeIndexResult result = indexService.index(20L);

        assertThat(result.meetingId()).isEqualTo(20L);
        assertThat(result.indexedChunkCount()).isEqualTo(2);
        assertThat(result.skippedSourceCount()).isZero();
    }

    @Test
    void mapsSkippedSynchronizationToMeetingResult() {
        KnowledgeSourceContent source = new KnowledgeSourceContent(
                20L, 10L, SourceType.TRANSCRIPT, "weekly meeting", "same transcript"
        );
        when(sourceReader.read(20L)).thenReturn(source);
        when(knowledgeIndexService.synchronize(source)).thenReturn(KnowledgeIndexResult.skippedResult());

        MeetingKnowledgeIndexResult result = indexService.index(20L);

        assertThat(result.indexedChunkCount()).isZero();
        assertThat(result.skippedSourceCount()).isEqualTo(1);
    }
}
