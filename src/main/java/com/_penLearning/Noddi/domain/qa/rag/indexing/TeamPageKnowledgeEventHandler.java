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

    private final TeamPageKnowledgeCoordinator coordinator;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChanged(TeamPageChangedEvent event) {
        try {
            KnowledgeIndexResult result = coordinator.synchronize(event.pageId());
            log.info(
                    "[TeamPageKnowledgeIndex] 동기화 완료: pageId={}, indexedChunkCount={}, skipped={}",
                    event.pageId(),
                    result.indexedChunkCount(),
                    result.skipped()
            );
        } catch (Exception exception) {
            // 공유페이지 저장은 이미 완료됐다. 실패 건은 복구 스케줄러가 DB 상태를 기준으로 다시 처리한다.
            log.error("[TeamPageKnowledgeIndex] 동기화 실패: pageId={}", event.pageId(), exception);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleted(TeamPageDeletedEvent event) {
        try {
            coordinator.delete(event.pageId());
        } catch (Exception exception) {
            // 원본 없이 남은 TEAM_TEXT 색인은 복구 스케줄러가 찾아 다시 삭제한다.
            log.error("[TeamPageKnowledgeIndex] 삭제 실패: pageId={}", event.pageId(), exception);
        }
    }
}
