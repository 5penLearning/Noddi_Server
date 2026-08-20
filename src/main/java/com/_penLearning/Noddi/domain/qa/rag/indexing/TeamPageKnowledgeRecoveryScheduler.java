package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.repository.QaKnowledgeIndexRepository;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 공유페이지 이벤트 유실과 일시적인 외부 API 장애를 주기적으로 복구한다.
 * 누락·변경된 페이지는 다시 색인하고, 원본 없이 남은 색인은 삭제한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamPageKnowledgeRecoveryScheduler {

    private static final int RECOVERY_BATCH_SIZE = 100;

    private final TeamPageRepository teamPageRepository;
    private final QaKnowledgeIndexRepository knowledgeIndexRepository;
    private final TeamPageKnowledgeCoordinator coordinator;

    @Scheduled(
            cron = "${scheduler.qa-knowledge.recovery-cron:0 */5 * * * *}",
            zone = "${scheduler.qa-knowledge.zone:Asia/Seoul}"
    )
    public void reconcileTeamPageKnowledge() {
        List<Long> synchronizationTargets = teamPageRepository
                .findIdsRequiringKnowledgeSynchronization(
                        SourceType.TEAM_TEXT,
                        PageRequest.of(0, RECOVERY_BATCH_SIZE)
                );

        for (Long pageId : synchronizationTargets) {
            try {
                coordinator.synchronize(pageId);
            } catch (Exception exception) {
                // 한 페이지의 장애가 같은 배치의 나머지 복구를 막지 않도록 건별로 격리한다.
                log.error(
                        "[TeamPageKnowledgeRecovery] 공유페이지 재색인 실패: pageId={}",
                        pageId,
                        exception
                );
            }
        }

        List<Long> deletionTargets = knowledgeIndexRepository.findOrphanedSourceIds(
                SourceType.TEAM_TEXT,
                PageRequest.of(0, RECOVERY_BATCH_SIZE)
        );

        for (Long pageId : deletionTargets) {
            try {
                coordinator.delete(pageId);
            } catch (Exception exception) {
                log.error(
                        "[TeamPageKnowledgeRecovery] 고아 색인 삭제 실패: pageId={}",
                        pageId,
                        exception
                );
            }
        }

        if (!synchronizationTargets.isEmpty() || !deletionTargets.isEmpty()) {
            log.info(
                    "[TeamPageKnowledgeRecovery] 정합성 복구 시도 완료: synchronizeCount={}, deleteCount={}",
                    synchronizationTargets.size(),
                    deletionTargets.size()
            );
        }
    }
}
