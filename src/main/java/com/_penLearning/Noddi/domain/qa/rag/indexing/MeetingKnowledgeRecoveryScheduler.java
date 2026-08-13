package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 회의 처리는 완료됐지만 Pinecone 색인 기록이 없는 전사를 주기적으로 재색인한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingKnowledgeRecoveryScheduler {

    private static final int RECOVERY_BATCH_SIZE = 100;

    private final MeetingSummaryRepository meetingSummaryRepository;
    private final MeetingKnowledgeIndexService meetingKnowledgeIndexService;

    @Scheduled(
            cron = "${scheduler.qa-knowledge.recovery-cron:0 */5 * * * *}",
            zone = "${scheduler.qa-knowledge.zone:Asia/Seoul}"
    )
    public void retryUnindexedMeetings() {
        List<Long> meetingIds = meetingSummaryRepository.findUnindexedMeetingIds(
                AiStatus.COMPLETED,
                SourceType.TRANSCRIPT,
                PageRequest.of(0, RECOVERY_BATCH_SIZE)
        );

        for (Long meetingId : meetingIds) {
            try {
                meetingKnowledgeIndexService.index(meetingId);
            } catch (Exception exception) {
                // 한 회의의 실패가 나머지 미색인 회의의 복구를 막지 않도록 건별로 격리한다.
                log.error(
                        "[MeetingKnowledgeRecovery] 전사 재색인 실패: meetingId={}",
                        meetingId,
                        exception
                );
            }
        }

        if (!meetingIds.isEmpty()) {
            log.info("[MeetingKnowledgeRecovery] 미색인 회의 복구 시도 완료: count={}", meetingIds.size());
        }
    }
}
