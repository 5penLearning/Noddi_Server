package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingKnowledgeRecoverySchedulerTest {

    @Mock
    private MeetingSummaryRepository meetingSummaryRepository;

    @Mock
    private MeetingKnowledgeIndexService meetingKnowledgeIndexService;

    @Test
    void retriesEveryUnindexedCompletedMeeting() {
        MeetingKnowledgeRecoveryScheduler scheduler = new MeetingKnowledgeRecoveryScheduler(
                meetingSummaryRepository,
                meetingKnowledgeIndexService
        );
        when(meetingSummaryRepository.findUnindexedMeetingIds(
                eq(AiStatus.COMPLETED),
                eq(SourceType.TRANSCRIPT),
                any(Pageable.class)
        )).thenReturn(List.of(1L, 2L));

        scheduler.retryUnindexedMeetings();

        verify(meetingKnowledgeIndexService).index(1L);
        verify(meetingKnowledgeIndexService).index(2L);
    }
}
