package com._penLearning.Noddi.domain.qa.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 OpenAI Embedding API와 Pinecone을 호출하는 수동 통합 테스트다.
 * 일반 단위 테스트와 달리 외부 API 키와 네트워크 연결이 필요하다.
 */
@SpringBootTest
@Disabled("실제 OpenAI/Pinecone 연동을 수동으로 확인할 때만 실행")
class KnowledgeRetrieverIntegrationTest {

    private static final String TEST_TEAM_ID = "999999";
    private static final String OTHER_TEAM_ID = "888888";

    private static final String DEPLOY_DOCUMENT_ID = "pinecone-test-deploy";
    private static final String DESIGN_DOCUMENT_ID = "pinecone-test-design";
    private static final String LUNCH_DOCUMENT_ID = "pinecone-test-lunch";

    private static final List<String> TEST_DOCUMENT_IDS = List.of(
            DEPLOY_DOCUMENT_ID,
            DESIGN_DOCUMENT_ID,
            LUNCH_DOCUMENT_ID
    );

    @Autowired
    private VectorStore vectorStore;

    @Test
    void returnsMostRelevantDocumentAndSeparatesOtherTeam() throws InterruptedException {
        boolean saved = false;

        List<Document> documents = List.of(
                Document.builder()
                        .id(DEPLOY_DOCUMENT_ID)
                        .text("""
                                8월 13일 백엔드 회의에서 노디 프로젝트의 배포 일정을 논의했습니다.
                                백엔드 서버의 최종 배포일은 8월 20일로 결정했습니다.
                                배포 전까지 API 테스트와 환경변수 설정을 완료하기로 했습니다.
                                """)
                        .metadata(commonMetadata("1", "백엔드 배포 회의"))
                        .build(),

                Document.builder()
                        .id(DESIGN_DOCUMENT_ID)
                        .text("""
                                디자인 회의에서 노디 메인 화면의 색상을 논의했습니다.
                                서비스의 주요 색상은 초록색으로 결정했습니다.
                                다음 작업으로 버튼과 메뉴의 디자인을 통일하기로 했습니다.
                                """)
                        .metadata(commonMetadata("2", "서비스 디자인 회의"))
                        .build(),

                Document.builder()
                        .id(LUNCH_DOCUMENT_ID)
                        .text("""
                                팀원들과 다음 회식 일정을 논의했습니다.
                                회식 장소는 회사 근처 고깃집으로 결정했습니다.
                                참석 인원을 확인한 뒤 예약하기로 했습니다.
                                """)
                        .metadata(commonMetadata("3", "팀 회식 회의"))
                        .build()
        );

        try {
            System.out.println("1. 테스트 문서 3개 Pinecone 저장 시작");
            vectorStore.add(documents);
            saved = true;
            System.out.println("2. Pinecone 저장 요청 완료");

            List<Document> results = searchUntilReflected(
                    "노디 백엔드 서버는 언제 배포하기로 했나요?",
                    TEST_TEAM_ID
            );

            printResults(results);

            assertThat(results)
                    .as("같은 팀의 문서가 검색되어야 합니다.")
                    .isNotEmpty();

            assertThat(results.getFirst().getId())
                    .as("배포 질문에는 백엔드 배포 문서가 가장 먼저 검색되어야 합니다.")
                    .isEqualTo(DEPLOY_DOCUMENT_ID);

            assertThat(results)
                    .as("검색 결과는 요청한 팀의 자료로만 구성되어야 합니다.")
                    .allSatisfy(result -> assertThat(result.getMetadata().get("team_id"))
                            .isEqualTo(TEST_TEAM_ID));

            List<Document> otherTeamResults = vectorStore.similaritySearch(
                    searchRequest("노디 백엔드 서버는 언제 배포하기로 했나요?", OTHER_TEAM_ID)
            );

            assertThat(otherTeamResults)
                    .as("다른 팀 ID로 검색하면 테스트 문서가 노출되지 않아야 합니다.")
                    .isEmpty();
        } finally {
            if (saved) {
                vectorStore.delete(TEST_DOCUMENT_IDS);
                System.out.println("4. 테스트 문서 3개 삭제 완료");
            }
        }
    }

    private List<Document> searchUntilReflected(String query, String teamId) throws InterruptedException {
        List<Document> results = List.of();

        // Pinecone에 저장된 벡터가 검색에 반영될 때까지 최대 20초 동안 재시도한다.
        for (int attempt = 1; attempt <= 10; attempt++) {
            Thread.sleep(2_000);
            System.out.println("3. 검색 시도: " + attempt + "/10");

            results = vectorStore.similaritySearch(searchRequest(query, teamId));
            System.out.println("검색 결과 개수: " + results.size());

            if (!results.isEmpty()) {
                break;
            }
        }

        return results;
    }

    private SearchRequest searchRequest(String query, String teamId) {
        return SearchRequest.builder()
                .query(query)
                .topK(3)
                // 실제 전사 데이터로 점수 분포를 확인하기 전까지 모든 후보를 허용한다.
                .similarityThreshold(0.0)
                .filterExpression("team_id == '" + teamId + "'")
                .build();
    }

    private Map<String, Object> commonMetadata(String sourceId, String sourceTitle) {
        return Map.of(
                "team_id", TEST_TEAM_ID,
                "source_id", sourceId,
                "source_type", "TRANSCRIPT",
                "source_title", sourceTitle,
                "chunk_index", 0
        );
    }

    private void printResults(List<Document> results) {
        for (int index = 0; index < results.size(); index++) {
            Document result = results.get(index);
            System.out.println("--- 검색 순위 " + (index + 1) + " ---");
            System.out.println("문서 ID: " + result.getId());
            System.out.println("검색 결과: " + result.getText());
            System.out.println("메타데이터: " + result.getMetadata());
            System.out.println("유사도: " + result.getScore());
        }
    }
}
