package com._penLearning.Noddi.domain.qa.integration;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.event.QaQuestionCreatedEvent;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagGeneration;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 OpenAI Embedding/Chat API와 Pinecone을 함께 호출하는 수동 통합 테스트다.
 * IntelliJ 실행 구성에 OPENAI_API_KEY, PINECONE_API_KEY, PINECONE_INDEX_NAME이 필요하다.
 */
@SpringBootTest(properties = {
        // 외부 API만 실제로 사용하고 애플리케이션 데이터는 실제 MySQL 대신 격리된 H2에 저장한다.
        "spring.datasource.url=jdbc:h2:mem:qa-rag-integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "spring.ai.vectorstore.pinecone.index-name=${PINECONE_INDEX_NAME}",
        "qa.rag.top-k=3",
        "qa.rag.similarity-threshold=0.0"
})
@ActiveProfiles("test")
@Tag("external")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PINECONE_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "PINECONE_INDEX_NAME", matches = ".+")
class QaRagPipelineIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString();
    private static final String TEST_TEAM_ID = Long.toString(
            100_000_000L + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 800_000_000L)
    );
    private static final String OTHER_TEAM_ID = Long.toString(
            1_000_000_000L + Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 800_000_000L)
    );
    private static final String TEAM_DOCUMENT_ID = "rag-answer-test-team-deploy-" + RUN_ID;
    private static final String OTHER_TEAM_DOCUMENT_ID = "rag-answer-test-other-team-deploy-" + RUN_ID;
    private static final String FLOW_DOCUMENT_ID = "rag-answer-flow-test-deploy-" + RUN_ID;
    private static final List<String> TEST_DOCUMENT_IDS = List.of(
            TEAM_DOCUMENT_ID,
            OTHER_TEAM_DOCUMENT_ID
    );

    /** IntelliJ 실행 구성의 DB 환경변수보다 높은 우선순위로 테스트 DB를 H2에 고정한다. */
    @DynamicPropertySource
    static void useIsolatedH2Database(DynamicPropertyRegistry registry) {
        String jdbcUrl = "jdbc:h2:mem:qa-rag-integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
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
    private QaRagAnswerGenerator answerGenerator;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private QaQuestionRepository questionRepository;

    @Autowired
    private QaAnswerRepository answerRepository;

    @Autowired
    private QaAnswerSourceRepository answerSourceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generatesAnswerOnlyFromRequestedTeamAndReturnsSources() throws InterruptedException {
        boolean saved = false;

        try {
            System.out.println("1. 테스트용 팀 자료 Pinecone 저장 시작");
            vectorStore.add(List.of(
                    document(
                            TEAM_DOCUMENT_ID,
                            TEST_TEAM_ID,
                            "101",
                            "노디 백엔드 배포 회의",
                            "노디 백엔드 서버의 최종 배포일은 8월 20일입니다. 배포 전 API 테스트를 완료합니다."
                    ),
                    document(
                            OTHER_TEAM_DOCUMENT_ID,
                            OTHER_TEAM_ID,
                            "201",
                            "다른 팀 배포 회의",
                            "다른 팀의 백엔드 서버 배포일은 9월 30일입니다."
                    )
            ));
            saved = true;

            waitUntilDocumentIsSearchable(
                    TEST_TEAM_ID,
                    TEAM_DOCUMENT_ID,
                    "노디 백엔드 서버 배포일"
            );

            System.out.println("2. 실제 RAG 답변 생성 시작");
            QaRagGeneration generation = answerGenerator.prepare(
                            Long.valueOf(TEST_TEAM_ID),
                            "노디 백엔드 서버는 언제 배포하나요?"
                    )
                    .block(Duration.ofSeconds(60));

            assertThat(generation).isNotNull();

            String answer = generation.answerChunks()
                    .collectList()
                    .map(chunks -> String.join("", chunks))
                    .block(Duration.ofSeconds(60));

            printResult(answer, generation.sources());

            // LLM은 의미가 같아도 공백이나 줄바꿈을 다르게 생성할 수 있으므로 정규화해서 검증한다.
            String normalizedAnswer = answer.replaceAll("\\s+", "");
            assertThat(normalizedAnswer)
                    .contains("8월20일")
                    .doesNotContain("9월30일");
            assertThat(generation.sources())
                    .isNotEmpty()
                    .allSatisfy(source -> assertThat(source.documentId())
                            .isNotEqualTo(OTHER_TEAM_DOCUMENT_ID))
                    .anySatisfy(source -> assertThat(source.documentId())
                            .isEqualTo(TEAM_DOCUMENT_ID));
        } finally {
            if (saved) {
                vectorStore.delete(TEST_DOCUMENT_IDS);
                System.out.println("4. 테스트 문서 삭제 완료");
            }
        }
    }

    @Test
    void savesGeneratedAnswerAndSourcesAfterQuestionEvent() throws InterruptedException {
        FlowFixture fixture = createFlowFixture();
        boolean saved = false;

        try {
            System.out.println("1. 종단 테스트용 팀 자료 Pinecone 저장 시작");
            vectorStore.add(List.of(document(
                    FLOW_DOCUMENT_ID,
                    fixture.teamId().toString(),
                    "301",
                    "QA 종단 테스트 배포 회의",
                    "QA 종단 테스트의 백엔드 서버 배포일은 8월 27일입니다."
            )));
            saved = true;
            waitUntilDocumentIsSearchable(
                    fixture.teamId().toString(),
                    FLOW_DOCUMENT_ID,
                    "QA 종단 테스트 백엔드 서버 배포일"
            );

            Long questionId = saveQuestionAndPublishEvent(fixture);
            System.out.println("2. 질문 저장 및 생성 이벤트 발행 완료: questionId=" + questionId);

            QaQuestion completedQuestion = waitUntilAnswerCompleted(questionId);
            assertThat(completedQuestion.getStatus()).isEqualTo(QaStatus.ANSWERED);

            QaAnswer answer = answerRepository.findByQuestion(completedQuestion)
                    .orElseThrow(() -> new AssertionError("AI 답변이 저장되지 않았습니다."));
            List<QaAnswerSource> sources = answerSourceRepository.findByAnswer_AnswerId(answer.getAnswerId());

            System.out.println("3. DB에 저장된 AI 답변: " + answer.getContent());
            System.out.println("DB에 저장된 출처 개수: " + sources.size());

            assertThat(answer.getContent().replaceAll("\\s+", ""))
                    .contains("8월27일");
            assertThat(sources)
                    .hasSize(1)
                    .first()
                    .satisfies(source -> {
                        assertThat(source.getSourceType()).isEqualTo(SourceType.TRANSCRIPT);
                        assertThat(source.getCitationIndex()).isEqualTo(1);
                        assertThat(source.getReferenceId()).isEqualTo(301L);
                        assertThat(source.getSourceTitle()).isEqualTo("QA 종단 테스트 배포 회의");
                        assertThat(source.getExcerpt()).contains("8월 27일");
                    });
        } finally {
            if (saved) {
                vectorStore.delete(List.of(FLOW_DOCUMENT_ID));
                System.out.println("4. 종단 테스트 문서 삭제 완료");
            }
        }
    }

    private Document document(
            String documentId,
            String teamId,
            String sourceId,
            String sourceTitle,
            String content
    ) {
        return Document.builder()
                .id(documentId)
                .text(content)
                .metadata(Map.of(
                        "team_id", teamId,
                        "source_id", sourceId,
                        "source_type", "TRANSCRIPT",
                        "source_title", sourceTitle,
                        "chunk_index", 0
                ))
                .build();
    }

    private void waitUntilDocumentIsSearchable(
            String teamId,
            String expectedDocumentId,
            String query
    ) throws InterruptedException {
        for (int attempt = 1; attempt <= 10; attempt++) {
            Thread.sleep(2_000);
            List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .similarityThreshold(0.0)
                    .filterExpression("team_id == '" + teamId + "'")
                    .build());

            System.out.println("Pinecone 반영 확인: " + attempt + "/10, 결과=" + results.size());
            if (results.stream().anyMatch(document -> document.getId().equals(expectedDocumentId))) {
                return;
            }
        }

        throw new IllegalStateException("Pinecone에 테스트 문서가 제한 시간 안에 반영되지 않았습니다.");
    }

    private FlowFixture createFlowFixture() {
        // 실제 Pinecone의 일반 팀 ID와 겹치지 않도록 H2의 테스트 팀 ID 대역을 분리한다.
        jdbcTemplate.execute("ALTER TABLE Team ALTER COLUMN team_id RESTART WITH 999990");

        return transactionTemplate.execute(status -> {
            Organization organization = organizationRepository.save(Organization.builder()
                    .name("QA 종단 테스트 조직")
                    .emailDomain("qa-flow-test.local")
                    .build());
            User user = userRepository.save(User.builder()
                    .organization(organization)
                    .email("qa-flow-test@qa-flow-test.local")
                    .name("QA 테스터")
                    .password("encoded-test-password")
                    .build());
            Project project = projectRepository.save(Project.create(
                    "QA 종단 테스트 프로젝트",
                    "RAG 이벤트 흐름 검증",
                    organization,
                    user
            ));
            Team team = teamRepository.save(Team.builder()
                    .project(project)
                    .name("QA 종단 테스트 팀")
                    .description("RAG 이벤트 흐름 검증")
                    .createdBy(user)
                    .build());

            return new FlowFixture(user.getUserId(), team.getTeamId());
        });
    }

    private Long saveQuestionAndPublishEvent(FlowFixture fixture) {
        return transactionTemplate.execute(status -> {
            User questioner = userRepository.findById(fixture.userId()).orElseThrow();
            Team targetTeam = teamRepository.findById(fixture.teamId()).orElseThrow();
            QaQuestion question = questionRepository.save(QaQuestion.builder()
                    .questioner(questioner)
                    .targetTeam(targetTeam)
                    .content("QA 종단 테스트의 백엔드 서버는 언제 배포하나요?")
                    .build());

            eventPublisher.publishEvent(new QaQuestionCreatedEvent(question.getQuestionId()));
            return question.getQuestionId();
        });
    }

    private QaQuestion waitUntilAnswerCompleted(Long questionId) throws InterruptedException {
        for (int attempt = 1; attempt <= 30; attempt++) {
            Thread.sleep(2_000);
            QaQuestion question = questionRepository.findById(questionId).orElseThrow();
            System.out.println("AI 답변 상태 확인: " + attempt + "/30, status=" + question.getStatus());

            if (question.getStatus() == QaStatus.ANSWERED || question.getStatus() == QaStatus.FAILED) {
                return question;
            }
        }

        throw new IllegalStateException("AI 답변 생성이 제한 시간 안에 완료되지 않았습니다.");
    }

    private void printResult(String answer, List<RetrievedKnowledge> sources) {
        System.out.println("3. 생성된 AI 답변: " + answer);
        System.out.println("사용된 근거 개수: " + sources.size());
        for (int index = 0; index < sources.size(); index++) {
            RetrievedKnowledge source = sources.get(index);
            System.out.println("[근거 " + (index + 1) + "] " + source.sourceTitle());
            System.out.println(source.content());
        }
    }

    private record FlowFixture(Long userId, Long teamId) {
    }
}
