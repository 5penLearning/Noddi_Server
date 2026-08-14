package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.meeting.event.MeetingTranscriptReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 전사 저장 트랜잭션이 커밋된 뒤 회의 원문을 Pinecone에 색인한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingTranscriptReadyEventHandler {

    private final MeetingKnowledgeIndexService meetingKnowledgeIndexService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MeetingTranscriptReadyEvent event) {
        try {
            MeetingKnowledgeIndexResult result = meetingKnowledgeIndexService.index(event.meetingId());
            log.info(
                    "[MeetingKnowledgeIndex] 전사 색인 이벤트 처리 완료: meetingId={}, indexedChunkCount={}, skippedSourceCount={}",
                    event.meetingId(),
                    result.indexedChunkCount(),
                    result.skippedSourceCount()
            );
        } catch (Exception exception) {
            // 회의 전사와 요약은 이미 정상 저장됐으므로 색인 실패가 회의 AI 상태를 FAILED로 되돌리면 안 된다.
            log.error(
                    "[MeetingKnowledgeIndex] 전사 색인 실패: meetingId={}",
                    event.meetingId(),
                    exception
            );
        }
    }
}
