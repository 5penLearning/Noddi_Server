package com._penLearning.Noddi.global.config.demo;

import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeIndexResult;
import com._penLearning.Noddi.domain.qa.rag.indexing.MeetingKnowledgeIndexResult;
import com._penLearning.Noddi.domain.qa.rag.indexing.MeetingKnowledgeIndexService;
import com._penLearning.Noddi.domain.qa.rag.indexing.TeamPageKnowledgeCoordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEMO_DATA_ENABLED=true일 때만 시연 데이터를 만들고 Pinecone 색인을 실행한다.
 *
 * DB 생성 트랜잭션이 커밋된 뒤 기존 운영 색인 서비스를 직접 호출하므로,
 * 시연 데이터도 실제 전사/공유페이지와 동일한 청킹·메타데이터 규칙을 사용한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "demo-data", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoDataSeedService seedService;
    private final MeetingKnowledgeIndexService meetingKnowledgeIndexService;
    private final TeamPageKnowledgeCoordinator teamPageKnowledgeCoordinator;
    private final String demoPassword;

    public DemoDataInitializer(
            DemoDataSeedService seedService,
            MeetingKnowledgeIndexService meetingKnowledgeIndexService,
            TeamPageKnowledgeCoordinator teamPageKnowledgeCoordinator,
            @Value("${demo-data.password}") String demoPassword
    ) {
        this.seedService = seedService;
        this.meetingKnowledgeIndexService = meetingKnowledgeIndexService;
        this.teamPageKnowledgeCoordinator = teamPageKnowledgeCoordinator;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("[DemoData] 시연 데이터 초기화를 시작합니다. 운영 완료 후 DEMO_DATA_ENABLED=false로 변경하세요.");

        DemoDataSeedService.SeedResult seedResult = seedService.seed(demoPassword);
        if (!seedResult.created()) {
            log.info("[DemoData] 기존 시연 데이터를 재사용하고 누락된 Pinecone 색인을 다시 확인합니다.");
        }

        int failedMeetingCount = indexMeetings(seedResult.meetingIds());
        int failedTeamPageCount = indexTeamPages(seedResult.teamPageIds());

        logDemoAccounts();
        if (failedMeetingCount == 0 && failedTeamPageCount == 0) {
            log.info(
                    "[DemoData] 초기화 및 Pinecone 동기화 완료: transcriptCount={}, teamPageCount={}",
                    seedResult.meetingIds().size(),
                    seedResult.teamPageIds().size()
            );
            return;
        }

        log.error(
                "[DemoData] 일부 Pinecone 색인이 실패했습니다: failedTranscriptCount={}, failedTeamPageCount={}. "
                        + "원인을 해결한 뒤 DEMO_DATA_ENABLED=true 상태로 재시작하면 다시 시도합니다.",
                failedMeetingCount,
                failedTeamPageCount
        );
    }

    private int indexMeetings(Iterable<Long> meetingIds) {
        int failedCount = 0;
        for (Long meetingId : meetingIds) {
            try {
                MeetingKnowledgeIndexResult result = meetingKnowledgeIndexService.index(meetingId);
                log.info(
                        "[DemoData] 회의 전사 Pinecone 동기화 완료: meetingId={}, indexedChunkCount={}, skipped={}",
                        meetingId,
                        result.indexedChunkCount(),
                        result.skippedSourceCount() > 0
                );
            } catch (Exception exception) {
                failedCount++;
                log.error("[DemoData] 회의 전사 Pinecone 동기화 실패: meetingId={}", meetingId, exception);
            }
        }
        return failedCount;
    }

    private int indexTeamPages(Iterable<Long> teamPageIds) {
        int failedCount = 0;
        for (Long pageId : teamPageIds) {
            try {
                KnowledgeIndexResult result = teamPageKnowledgeCoordinator.synchronize(pageId);
                log.info(
                        "[DemoData] 공유페이지 Pinecone 동기화 완료: pageId={}, indexedChunkCount={}, skipped={}",
                        pageId,
                        result.indexedChunkCount(),
                        result.skipped()
                );
            } catch (Exception exception) {
                failedCount++;
                log.error("[DemoData] 공유페이지 Pinecone 동기화 실패: pageId={}", pageId, exception);
            }
        }
        return failedCount;
    }

    private void logDemoAccounts() {
        // 평문 비밀번호가 운영 로그에 남지 않도록 환경변수 이름만 안내한다.
        log.info("[DemoData] 공통 비밀번호는 DEMO_DATA_PASSWORD 환경변수 값입니다.");
        log.info("[DemoData] 전체 팀 조회용 계정: demo.pm@{}", DemoDataSeedService.DEMO_ORGANIZATION_DOMAIN);
        log.info("[DemoData] 백엔드팀 리더: demo.backend@{}", DemoDataSeedService.DEMO_ORGANIZATION_DOMAIN);
        log.info("[DemoData] 마케팅팀 리더: demo.marketing@{}", DemoDataSeedService.DEMO_ORGANIZATION_DOMAIN);
    }
}
