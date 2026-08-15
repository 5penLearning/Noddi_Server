package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.teamPage.event.TeamPageChangedEvent;
import com._penLearning.Noddi.domain.teamPage.event.TeamPageDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 공유페이지 트랜잭션이 확정된 뒤 Pinecone 벡터와 MySQL 인덱싱 상태를 비동기로 동기화한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamPageKnowledgeEventHandler {

    private final TeamPageKnowledgeIndexService indexService;
    private final KnowledgeDeletionService deletionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChanged(TeamPageChangedEvent event) {
        try {
            KnowledgeIndexResult result = indexService.index(event.pageId());
            log.info(
                    "[TeamPageKnowledgeIndex] 동기화 완료: pageId={}, indexedChunkCount={}, skipped={}",
                    event.pageId(),
                    result.indexedChunkCount(),
                    result.skipped()
            );
        } catch (Exception exception) {
            // 공유페이지 저장은 이미 완료되었으므로 인덱싱 실패를 분리해서 기록한다.
            log.error("[TeamPageKnowledgeIndex] 동기화 실패: pageId={}", event.pageId(), exception);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleted(TeamPageDeletedEvent event) {
        try {
            deletionService.deleteTeamPageKnowledge(event.pageId());
        } catch (Exception exception) {
            log.error("[TeamPageKnowledgeIndex] 삭제 실패: pageId={}", event.pageId(), exception);
        }
    }
}
