package com._penLearning.Noddi.global.config.demo;

import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeIndexResult;
import com._penLearning.Noddi.domain.qa.rag.indexing.MeetingKnowledgeIndexResult;
import com._penLearning.Noddi.domain.qa.rag.indexing.MeetingKnowledgeIndexService;
import com._penLearning.Noddi.domain.qa.rag.indexing.TeamPageKnowledgeCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

    private static final String DEMO_PASSWORD = "Demo1234!";

    @Mock
    private DemoDataSeedService seedService;
    @Mock
    private MeetingKnowledgeIndexService meetingKnowledgeIndexService;
    @Mock
    private TeamPageKnowledgeCoordinator teamPageKnowledgeCoordinator;

    private DemoDataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DemoDataInitializer(
                seedService,
                meetingKnowledgeIndexService,
                teamPageKnowledgeCoordinator,
                DEMO_PASSWORD
        );
    }

    @Test
    void indexesEverySeededTranscriptAndTeamPage() {
        when(seedService.seed(DEMO_PASSWORD)).thenReturn(new DemoDataSeedService.SeedResult(
                true,
                List.of(11L, 12L, 13L),
                List.of(21L, 22L)
        ));
        when(meetingKnowledgeIndexService.index(11L))
                .thenReturn(new MeetingKnowledgeIndexResult(11L, 1, 0));
        when(meetingKnowledgeIndexService.index(12L))
                .thenReturn(new MeetingKnowledgeIndexResult(12L, 1, 0));
        when(meetingKnowledgeIndexService.index(13L))
                .thenReturn(new MeetingKnowledgeIndexResult(13L, 1, 0));
        when(teamPageKnowledgeCoordinator.synchronize(21L))
                .thenReturn(KnowledgeIndexResult.indexed(1));
        when(teamPageKnowledgeCoordinator.synchronize(22L))
                .thenReturn(KnowledgeIndexResult.indexed(1));

        initializer.run(mock(ApplicationArguments.class));

        verify(meetingKnowledgeIndexService).index(11L);
        verify(meetingKnowledgeIndexService).index(12L);
        verify(meetingKnowledgeIndexService).index(13L);
        verify(teamPageKnowledgeCoordinator).synchronize(21L);
        verify(teamPageKnowledgeCoordinator).synchronize(22L);
    }

    @Test
    void continuesIndexingRemainingSourcesWhenOneSourceFails() {
        when(seedService.seed(DEMO_PASSWORD)).thenReturn(new DemoDataSeedService.SeedResult(
                false,
                List.of(11L, 12L),
                List.of(21L)
        ));
        when(meetingKnowledgeIndexService.index(11L))
                .thenThrow(new IllegalStateException("temporary Pinecone failure"));
        when(meetingKnowledgeIndexService.index(12L))
                .thenReturn(new MeetingKnowledgeIndexResult(12L, 1, 0));
        when(teamPageKnowledgeCoordinator.synchronize(21L))
                .thenReturn(KnowledgeIndexResult.indexed(1));

        initializer.run(mock(ApplicationArguments.class));

        verify(meetingKnowledgeIndexService).index(12L);
        verify(teamPageKnowledgeCoordinator).synchronize(21L);
    }

    @Test
    void doesNotCallIndexersWhenThereAreNoDemoSources() {
        when(seedService.seed(DEMO_PASSWORD)).thenReturn(new DemoDataSeedService.SeedResult(
                false,
                List.of(),
                List.of()
        ));

        initializer.run(mock(ApplicationArguments.class));

        verify(meetingKnowledgeIndexService, never()).index(org.mockito.ArgumentMatchers.anyLong());
        verify(teamPageKnowledgeCoordinator, never()).synchronize(org.mockito.ArgumentMatchers.anyLong());
    }
}
