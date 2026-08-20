package com._penLearning.Noddi.domain.qa.integration;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.qa.entity.QaKnowledgeIndex;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeDeletionService;
import com._penLearning.Noddi.domain.qa.repository.QaKnowledgeIndexRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import com._penLearning.Noddi.domain.teamPage.event.TeamPageChangedEvent;
import com._penLearning.Noddi.domain.teamPage.event.TeamPageDeletedEvent;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공유페이지 변경 이벤트부터 OpenAI 임베딩과 Pinecone 저장·검색·삭제까지 확인하는 수동 종단 테스트다.
 * IntelliJ 실행 구성에 OPENAI_API_KEY, PINECONE_API_KEY, PINECONE_INDEX_NAME이 필요하다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:team-page-knowledge-integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.model.chat=openai",
        "spring.ai.model.embedding=openai",
        "spring.ai.vectorstore.type=pinecone",
        "spring.ai.openai.api-key=${OPENAI_API_KEY}",
        "spring.ai.openai.chat.model=${OPENAI_CHAT_MODEL:gpt-4o-mini}",
        "spring.ai.openai.embedding.model=${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}",
        "spring.ai.openai.embedding.dimensions=${OPENAI_EMBEDDING_DIMENSIONS:1536}",
        "spring.ai.vectorstore.pinecone.api-key=${PINECONE_API_KEY}",
        "spring.ai.vectorstore.pinecone.index-name=${PINECONE_INDEX_NAME}"
})
@ActiveProfiles("test")
@Tag("external")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PINECONE_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PINECONE_INDEX_NAME", matches = ".+")
class TeamPageKnowledgeIndexIntegrationTest {

    private static final long TEST_PAGE_ID_START = randomIdInRange(1_500_000_000L);
    private static final long TEST_TEAM_ID_START = randomIdInRange(1_700_000_000L);
    private static final String OTHER_TEAM_ID = Long.toString(randomIdInRange(1_900_000_000L));
    private static final String INITIAL_TITLE = "피닉스 배포 운영 규칙";
    private static final String INITIAL_CONTENT = """
            ## 정기 배포 일정
            피닉스 백엔드는 매주 금요일 오후 4시에 배포합니다.
            배포 담당자는 체크리스트를 확인한 뒤 승인을 요청합니다.
            """;
    private static final String UPDATED_TITLE = "피닉스 긴급 배포 운영 규칙";
    private static final String UPDATED_CONTENT = """
            ## 변경된 배포 일정
            피닉스 백엔드 긴급 배포는 매주 목요일 오후 3시에 진행합니다.
            운영 책임자의 승인을 받은 뒤 배포합니다.
            """;

    @DynamicPropertySource
    static void useIsolatedH2Database(DynamicPropertyRegistry registry) {
        String jdbcUrl = "jdbc:h2:mem:team-page-knowledge-integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.hikari.jdbc-url", () -> jdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamPageRepository teamPageRepository;
    @Autowired
    private QaKnowledgeIndexRepository knowledgeIndexRepository;
    @Autowired
    private KnowledgeDeletionService deletionService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void synchronizesTeamPageWithPineconeAfterCreateUpdateAndDeleteEvents() throws InterruptedException {
        PageFixture fixture = createPageAndPublishChangedEvent();
        String documentId = documentId(fixture.pageId());

        try {
            System.out.println("1. 공유페이지 생성 이벤트 발행 완료: pageId=" + fixture.pageId());
            QaKnowledgeIndex initialIndex = waitUntilIndexStateExists(fixture.pageId());
            Document initialDocument = waitUntilDocumentIsSearchable(
                    fixture.teamId(),
                    documentId,
                    "피닉스 백엔드 정기 배포 시간"
            );

            System.out.println("2. 생성된 공유페이지 Pinecone 검색 성공");
            printDocument(initialDocument);
            assertTeamTextMetadata(initialDocument, fixture, INITIAL_TITLE);
            assertThat(initialDocument.getText())
                    .contains("# " + INITIAL_TITLE)
                    .contains("금요일 오후 4시");
            assertDocumentIsNotExposedToOtherTeam(documentId);

            updatePageAndPublishChangedEvent(fixture.pageId());
            waitUntilIndexHashChanges(fixture.pageId(), initialIndex.getContentHash());
            Document updatedDocument = waitUntilDocumentContains(
                    fixture.teamId(),
                    documentId,
                    "피닉스 백엔드 긴급 배포 시간",
                    "목요일 오후 3시"
            );

            System.out.println("3. 수정된 공유페이지 Pinecone 갱신 성공");
            printDocument(updatedDocument);
            assertTeamTextMetadata(updatedDocument, fixture, UPDATED_TITLE);
            assertThat(updatedDocument.getText())
                    .contains("# " + UPDATED_TITLE)
                    .contains("목요일 오후 3시")
                    .doesNotContain("금요일 오후 4시");

            deletePageAndPublishDeletedEvent(fixture.pageId());
            waitUntilIndexStateIsDeleted(fixture.pageId());
            waitUntilDocumentIsDeleted(fixture.teamId(), documentId);
            System.out.println("4. 삭제된 공유페이지 Pinecone 청크 정리 성공");
        } finally {
            // 중간 검증이 실패해도 공용 Pinecone 인덱스에 테스트 벡터가 남지 않게 정리한다.
            deletionService.deleteTeamPageKnowledge(fixture.pageId());
            vectorStore.delete(List.of(documentId));
        }
    }

    private PageFixture createPageAndPublishChangedEvent() {
        jdbcTemplate.execute("ALTER TABLE team_page ALTER COLUMN page_id RESTART WITH " + TEST_PAGE_ID_START);
        jdbcTemplate.execute("ALTER TABLE Team ALTER COLUMN team_id RESTART WITH " + TEST_TEAM_ID_START);
        String runId = UUID.randomUUID().toString();

        return transactionTemplate.execute(status -> {
            Organization organization = organizationRepository.save(Organization.builder()
                    .name("공유페이지 RAG 테스트 조직")
                    .emailDomain("team-page-" + runId + ".local")
                    .build());
            User author = userRepository.save(User.builder()
                    .organization(organization)
                    .email("author@team-page-" + runId + ".local")
                    .name("공유페이지 작성자")
                    .password("encoded-test-password")
                    .build());
            Project project = projectRepository.save(Project.create(
                    "공유페이지 RAG 테스트 프로젝트",
                    "공유페이지 Pinecone 종단 테스트",
                    organization,
                    author
            ));
            Team team = teamRepository.save(Team.builder()
                    .project(project)
                    .name("공유페이지 RAG 테스트 팀")
                    .description("공유페이지 Pinecone 종단 테스트")
                    .createdBy(author)
                    .build());
            TeamPage page = teamPageRepository.save(TeamPage.builder()
                    .team(team)
                    .author(author)
                    .title(INITIAL_TITLE)
                    .content(INITIAL_CONTENT)
                    .build());

            eventPublisher.publishEvent(new TeamPageChangedEvent(page.getPageId()));
            return new PageFixture(page.getPageId(), team.getTeamId());
        });
    }

    private void updatePageAndPublishChangedEvent(Long pageId) {
        transactionTemplate.executeWithoutResult(status -> {
            TeamPage page = teamPageRepository.findById(pageId).orElseThrow();
            page.update(UPDATED_TITLE, UPDATED_CONTENT);
            eventPublisher.publishEvent(new TeamPageChangedEvent(pageId));
        });
    }

    private void deletePageAndPublishDeletedEvent(Long pageId) {
        transactionTemplate.executeWithoutResult(status -> {
            teamPageRepository.deleteById(pageId);
            eventPublisher.publishEvent(new TeamPageDeletedEvent(pageId));
        });
    }

    private QaKnowledgeIndex waitUntilIndexStateExists(Long pageId) throws InterruptedException {
        for (int attempt = 1; attempt <= 20; attempt++) {
            QaKnowledgeIndex index = knowledgeIndexRepository
                    .findBySourceIdAndSourceType(pageId, SourceType.TEAM_TEXT)
                    .orElse(null);
            System.out.println("인덱싱 상태 확인: " + attempt + "/20, indexed=" + (index != null));
            if (index != null) {
                return index;
            }
            Thread.sleep(1_000);
        }
        throw new IllegalStateException("공유페이지 인덱싱 상태가 제한 시간 안에 저장되지 않았습니다.");
    }

    private void waitUntilIndexHashChanges(Long pageId, String previousHash) throws InterruptedException {
        for (int attempt = 1; attempt <= 20; attempt++) {
            String currentHash = knowledgeIndexRepository
                    .findBySourceIdAndSourceType(pageId, SourceType.TEAM_TEXT)
                    .map(QaKnowledgeIndex::getContentHash)
                    .orElse(null);
            System.out.println("수정 인덱싱 상태 확인: " + attempt + "/20");
            if (currentHash != null && !currentHash.equals(previousHash)) {
                return;
            }
            Thread.sleep(1_000);
        }
        throw new IllegalStateException("수정된 공유페이지 인덱싱 상태가 제한 시간 안에 반영되지 않았습니다.");
    }

    private Document waitUntilDocumentIsSearchable(
            Long teamId,
            String expectedDocumentId,
            String query
    ) throws InterruptedException {
        for (int attempt = 1; attempt <= 10; attempt++) {
            Thread.sleep(2_000);
            List<Document> results = search(teamId.toString(), query);
            System.out.println("Pinecone 생성 반영 확인: " + attempt + "/10, 결과=" + results.size());
            Document document = findDocument(results, expectedDocumentId);
            if (document != null) {
                return document;
            }
        }
        throw new IllegalStateException("공유페이지 청크가 제한 시간 안에 Pinecone 검색에 반영되지 않았습니다.");
    }

    private Document waitUntilDocumentContains(
            Long teamId,
            String expectedDocumentId,
            String query,
            String expectedText
    ) throws InterruptedException {
        for (int attempt = 1; attempt <= 10; attempt++) {
            Thread.sleep(2_000);
            Document document = findDocument(search(teamId.toString(), query), expectedDocumentId);
            System.out.println("Pinecone 수정 반영 확인: " + attempt + "/10");
            if (document != null && document.getText().contains(expectedText)) {
                return document;
            }
        }
        throw new IllegalStateException("수정된 공유페이지 청크가 제한 시간 안에 Pinecone 검색에 반영되지 않았습니다.");
    }

    private void waitUntilIndexStateIsDeleted(Long pageId) throws InterruptedException {
        for (int attempt = 1; attempt <= 20; attempt++) {
            boolean exists = knowledgeIndexRepository
                    .findBySourceIdAndSourceType(pageId, SourceType.TEAM_TEXT)
                    .isPresent();
            if (!exists) {
                return;
            }
            Thread.sleep(1_000);
        }
        throw new IllegalStateException("삭제된 공유페이지 인덱싱 상태가 제한 시간 안에 제거되지 않았습니다.");
    }

    private void waitUntilDocumentIsDeleted(Long teamId, String expectedDocumentId) throws InterruptedException {
        for (int attempt = 1; attempt <= 10; attempt++) {
            Thread.sleep(2_000);
            List<Document> results = search(teamId.toString(), "피닉스 백엔드 배포 규칙");
            System.out.println("Pinecone 삭제 반영 확인: " + attempt + "/10, 결과=" + results.size());
            if (findDocument(results, expectedDocumentId) == null) {
                return;
            }
        }
        throw new IllegalStateException("삭제된 공유페이지 청크가 제한 시간 안에 Pinecone에서 제거되지 않았습니다.");
    }

    private void assertDocumentIsNotExposedToOtherTeam(String expectedDocumentId) {
        assertThat(search(OTHER_TEAM_ID, "피닉스 백엔드 정기 배포 시간"))
                .noneMatch(document -> expectedDocumentId.equals(document.getId()));
    }

    private List<Document> search(String teamId, String query) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(0.0)
                .filterExpression("team_id == '" + teamId + "'")
                .build());
    }

    private Document findDocument(List<Document> documents, String expectedDocumentId) {
        return documents.stream()
                .filter(document -> expectedDocumentId.equals(document.getId()))
                .findFirst()
                .orElse(null);
    }

    private void assertTeamTextMetadata(Document document, PageFixture fixture, String expectedTitle) {
        assertThat(document.getMetadata())
                .containsEntry("team_id", fixture.teamId().toString())
                .containsEntry("source_id", fixture.pageId().toString())
                .containsEntry("source_type", SourceType.TEAM_TEXT.name())
                .containsEntry("source_title", expectedTitle);
        assertThat(((Number) document.getMetadata().get("chunk_index")).intValue()).isZero();
    }

    private void printDocument(Document document) {
        System.out.println("문서 ID: " + document.getId());
        System.out.println("검색 결과: " + document.getText());
        System.out.println("메타데이터: " + document.getMetadata());
    }

    private String documentId(Long pageId) {
        return "knowledge-team_text-" + pageId + "-0";
    }

    private static long randomIdInRange(long rangeStart) {
        return rangeStart + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 90_000_000L);
    }

    private record PageFixture(Long pageId, Long teamId) {
    }
}
