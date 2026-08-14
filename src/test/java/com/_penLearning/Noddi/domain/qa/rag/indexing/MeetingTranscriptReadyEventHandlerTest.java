package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.meeting.event.MeetingTranscriptReadyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTranscriptReadyEventHandlerTest {

    @Mock
    private MeetingKnowledgeIndexService indexService;

    @Test
    void indexesMeetingTranscriptWhenReadyEventIsReceived() {
        MeetingTranscriptReadyEventHandler handler = new MeetingTranscriptReadyEventHandler(indexService);
        when(indexService.index(20L)).thenReturn(new MeetingKnowledgeIndexResult(20L, 3, 0));

        handler.handle(new MeetingTranscriptReadyEvent(20L));

        verify(indexService).index(20L);
    }

    @Test
    void doesNotPropagateIndexFailureToMeetingProcessing() {
        MeetingTranscriptReadyEventHandler handler = new MeetingTranscriptReadyEventHandler(indexService);
        when(indexService.index(20L)).thenThrow(new RuntimeException("Pinecone failure"));

        assertThatCode(() -> handler.handle(new MeetingTranscriptReadyEvent(20L)))
                .doesNotThrowAnyException();
    }
}
