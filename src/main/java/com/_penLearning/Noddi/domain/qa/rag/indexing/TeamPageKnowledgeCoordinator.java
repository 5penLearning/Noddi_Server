package com._penLearning.Noddi.domain.qa.rag.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 같은 공유페이지에서 발생한 색인과 삭제 작업이 동시에 실행되지 않도록 조정한다.
 *
 * 현재 서버는 단일 애플리케이션 인스턴스로 운영하므로 JVM 내부 striped lock을 사용한다.
 * pageId마다 lock을 계속 생성하는 대신 고정된 lock 배열을 사용해 메모리 누수를 방지한다.
 */
@Component
@RequiredArgsConstructor
public class TeamPageKnowledgeCoordinator {

    private static final int LOCK_STRIPE_COUNT = 256;

    private final TeamPageKnowledgeIndexService indexService;
    private final KnowledgeDeletionService deletionService;
    private final ReentrantLock[] locks = createLocks();

    public KnowledgeIndexResult synchronize(Long pageId) {
        ReentrantLock lock = lockFor(pageId);
        lock.lock();
        try {
            return indexService.index(pageId);
        } finally {
            lock.unlock();
        }
    }

    public void delete(Long pageId) {
        ReentrantLock lock = lockFor(pageId);
        lock.lock();
        try {
            deletionService.deleteTeamPageKnowledge(pageId);
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lockFor(Long pageId) {
        int stripeIndex = Math.floorMod(Long.hashCode(pageId), LOCK_STRIPE_COUNT);
        return locks[stripeIndex];
    }

    private ReentrantLock[] createLocks() {
        ReentrantLock[] createdLocks = new ReentrantLock[LOCK_STRIPE_COUNT];
        for (int index = 0; index < LOCK_STRIPE_COUNT; index++) {
            createdLocks[index] = new ReentrantLock();
        }
        return createdLocks;
    }
}
