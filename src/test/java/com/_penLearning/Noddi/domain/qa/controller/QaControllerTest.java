package com._penLearning.Noddi.domain.qa.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.RevisionEditorType;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.service.QaAnswerRevisionQueryService;
import com._penLearning.Noddi.domain.qa.service.QaAnswerStreamSubscriptionService;
import com._penLearning.Noddi.domain.qa.service.QaAnswerUpdateService;
import com._penLearning.Noddi.domain.qa.service.QaQuestionCommandService;
import com._penLearning.Noddi.domain.qa.service.QaQuestionQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QaControllerTest {

    /*
     * 이 테스트는 HTTP 요청이 QaController를 거쳐 서비스 호출과 JSON 응답으로 이어지는지 확인한다.
     * 실제 비즈니스 규칙은 QaQuestionQueryServiceTest에서 검증했으므로 여기서는 다음 경계만 확인한다.
     *
     * 1. URL의 teamId와 query parameter가 올바르게 바인딩된다.
     * 2. 로그인 사용자의 userId가 @AuthenticationPrincipal에서 추출된다.
     * 3. 컨트롤러가 서비스 반환값을 공통 ApiResponse 형식으로 감싸서 응답한다.
     */

    @Mock
    private QaQuestionCommandService qaQuestionCommandService;

    @Mock
    private QaQuestionQueryService qaQuestionQueryService;

    @Mock
    private QaAnswerUpdateService qaAnswerUpdateService;

    @Mock
    private QaAnswerRevisionQueryService qaAnswerRevisionQueryService;

    @Mock
    private QaAnswerStreamSubscriptionService qaAnswerStreamSubscriptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        QaController controller = new QaController(
                qaQuestionCommandService,
                qaQuestionQueryService,
                qaAnswerUpdateService,
                qaAnswerRevisionQueryService,
                qaAnswerStreamSubscriptionService
        );

        // 실제 JWT 필터를 실행하는 대신 로그인 완료 후 SecurityContext에 들어갈 인증 객체를 준비한다.
        // AuthenticationPrincipalArgumentResolver가 이 principal을 읽어 컨트롤러의 AuthMember 인자로 전달한다.
        AuthMember authMember = AuthMember.builder()
                .userId(1L)
                .email("requester@noddi.com")
                .build();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(authMember, null, List.of())
        );
        SecurityContextHolder.setContext(securityContext);

        // Spring 전체 애플리케이션을 띄우지 않고 QaController만 MockMvc에 연결한다.
        // 따라서 이 테스트는 빠르게 HTTP 매핑과 응답 직렬화에만 집중할 수 있다.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        // SecurityContextHolder는 ThreadLocal을 사용하므로 다음 테스트에 인증 정보가 남지 않게 정리한다.
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTeamQaFeedWithDefaultPagination() throws Exception {
        // Given: 서비스가 질문, AI 답변, 답변 근거가 결합된 피드를 반환한다.
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 15, 10, 0);
        QaResponseDto.Feed response = createAnsweredFeed(createdAt);

        // cursor를 생략하고 size도 보내지 않으면 컨트롤러의 기본값인 20이 서비스에 전달되어야 한다.
        when(qaQuestionQueryService.getTeamFeed(1L, 10L, null, 20))
                .thenReturn(response);

        // When & Then: 팀 Q&A 피드 API를 호출하면 공통 응답 형식과 피드 내용이 JSON으로 반환된다.
        mockMvc.perform(get("/api/v1/teams/{teamId}/qa/feed", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("팀 Q&A 피드 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.projectId").value(100L))
                .andExpect(jsonPath("$.result.projectName").value("노디프로젝트"))
                .andExpect(jsonPath("$.result.teamId").value(10L))
                .andExpect(jsonPath("$.result.teamName").value("마케팅팀"))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.nextCursor").isEmpty())
                .andExpect(jsonPath("$.result.items[0].question.questionId").value(101L))
                .andExpect(jsonPath("$.result.items[0].question.questionerName").value("김유진"))
                .andExpect(jsonPath("$.result.items[0].status").value("ANSWERED"))
                .andExpect(jsonPath("$.result.items[0].answer.answerId").value(201L))
                .andExpect(jsonPath("$.result.items[0].answer.answerType").value("AI"))
                .andExpect(jsonPath("$.result.items[0].answer.sources[0].citationIndex").value(1))
                .andExpect(jsonPath("$.result.items[0].answer.sources[0].sourceTitle")
                        .value("8월 기획 회의"));

        // 로그인 사용자 ID, 경로의 팀 ID, 기본 페이지 조건이 서비스에 그대로 전달됐는지 확인한다.
        verify(qaQuestionQueryService).getTeamFeed(1L, 10L, null, 20);
    }

    @Test
    void passesCursorAndSizeQueryParametersToService() throws Exception {
        // Given: 이전 응답의 nextCursor=81을 사용해 과거 질문 10개를 요청하는 상황이다.
        QaResponseDto.Feed response = QaResponseDto.Feed.builder()
                .projectId(100L)
                .projectName("노디프로젝트")
                .teamId(10L)
                .teamName("마케팅팀")
                .items(List.of())
                .nextCursor(70L)
                .hasNext(true)
                .build();

        when(qaQuestionQueryService.getTeamFeed(1L, 10L, 81L, 10))
                .thenReturn(response);

        // When: 클라이언트가 cursor와 size를 query parameter로 명시해 다음 페이지를 요청한다.
        mockMvc.perform(get("/api/v1/teams/{teamId}/qa/feed", 10L)
                        .param("cursor", "81")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray())
                .andExpect(jsonPath("$.result.items").isEmpty())
                .andExpect(jsonPath("$.result.nextCursor").value(70L))
                .andExpect(jsonPath("$.result.hasNext").value(true));

        // Then: 문자열 query parameter가 Long과 int로 변환되어 서비스에 정확히 전달된다.
        verify(qaQuestionQueryService).getTeamFeed(1L, 10L, 81L, 10);
    }

    @Test
    void returnsAnswerRevisionHistoryForAuthenticatedProjectMember() throws Exception {
        // Given: 수정 이력 조회 서비스가 AI 최초 답변과 담당자 수정본을 반환한다.
        LocalDateTime aiCreatedAt = LocalDateTime.of(2026, 8, 17, 10, 0);
        LocalDateTime revisedAt = LocalDateTime.of(2026, 8, 17, 11, 0);

        QaResponseDto.AnswerSourceInfo source = QaResponseDto.AnswerSourceInfo.builder()
                .citationIndex(1)
                .sourceType(SourceType.TRANSCRIPT)
                .referenceId(301L)
                .sourceTitle("8월 기획 회의")
                .excerpt("출시일은 9월 5일입니다.")
                .build();

        QaResponseDto.AnswerRevisionItem version1 =
                QaResponseDto.AnswerRevisionItem.builder()
                        .versionNumber(1)
                        .content("AI 최초 답변")
                        .editorType(RevisionEditorType.AI)
                        .editorId(null)
                        .editorName("AI")
                        .createdAt(aiCreatedAt)
                        .sources(List.of(source))
                        .build();

        QaResponseDto.AnswerRevisionItem version2 =
                QaResponseDto.AnswerRevisionItem.builder()
                        .versionNumber(2)
                        .content("담당자가 수정한 최종 답변")
                        .editorType(RevisionEditorType.HUMAN)
                        .editorId(20L)
                        .editorName("홍길동")
                        .createdAt(revisedAt)
                        .sources(List.of())
                        .build();

        QaResponseDto.AnswerRevisionHistory response =
                QaResponseDto.AnswerRevisionHistory.builder()
                        .answerId(201L)
                        .totalVersions(2)
                        .canViewSources(true)
                        .revisions(List.of(version1, version2))
                        .build();

        when(qaAnswerRevisionQueryService.getAnswerRevisions(1L, 201L))
                .thenReturn(response);

        // When & Then: URL의 answerId와 인증 사용자의 ID가 서비스에 전달되고,
        // 프론트가 화살표 UI에 사용할 전체 버전 배열이 공통 응답 형식으로 반환된다.
        mockMvc.perform(get("/api/v1/qa/answers/{answerId}/revisions", 201L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("답변 수정 이력 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.answerId").value(201L))
                .andExpect(jsonPath("$.result.totalVersions").value(2))
                .andExpect(jsonPath("$.result.canViewSources").value(true))
                .andExpect(jsonPath("$.result.revisions[0].versionNumber").value(1))
                .andExpect(jsonPath("$.result.revisions[0].editorType").value("AI"))
                .andExpect(jsonPath("$.result.revisions[0].editorName").value("AI"))
                .andExpect(jsonPath("$.result.revisions[0].sources[0].referenceId").value(301L))
                .andExpect(jsonPath("$.result.revisions[1].versionNumber").value(2))
                .andExpect(jsonPath("$.result.revisions[1].editorType").value("HUMAN"))
                .andExpect(jsonPath("$.result.revisions[1].editorId").value(20L))
                .andExpect(jsonPath("$.result.revisions[1].editorName").value("홍길동"))
                .andExpect(jsonPath("$.result.revisions[1].sources").isEmpty());

        verify(qaAnswerRevisionQueryService).getAnswerRevisions(1L, 201L);
    }

    @Test
    void opensAnswerStreamForAuthenticatedUser() throws Exception {
        // Given: 구독 서비스가 질문 101번의 장시간 SSE 연결을 생성한다.
        SseEmitter emitter = new SseEmitter();
        when(qaAnswerStreamSubscriptionService.subscribe(1L, 101L))
                .thenReturn(emitter);

        // When & Then: SSE 엔드포인트를 호출하면 일반 JSON 응답으로 끝나지 않고
        // 비동기 요청을 시작해 이후 AI 이벤트를 계속 받을 수 있는 연결을 유지한다.
        mockMvc.perform(get("/api/v1/qa/questions/{questionId}/answer-stream", 101L)
                        .accept("text/event-stream"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        // SecurityContext의 로그인 사용자 ID와 URL의 질문 ID가 구독 서비스에 전달된다.
        verify(qaAnswerStreamSubscriptionService).subscribe(1L, 101L);

        // 테스트가 끝난 뒤 열려 있는 비동기 요청을 정리한다.
        emitter.complete();
    }

    private QaResponseDto.Feed createAnsweredFeed(LocalDateTime createdAt) {
        // 응답 JSON 구조 검증에 사용할 대표 데이터다.
        // Controller 테스트에서는 Entity 변환을 다시 시험하지 않고 완성된 DTO를 직접 만든다.
        QaResponseDto.AnswerSourceInfo source = QaResponseDto.AnswerSourceInfo.builder()
                .citationIndex(1)
                .sourceType(SourceType.TRANSCRIPT)
                .referenceId(301L)
                .sourceTitle("8월 기획 회의")
                .excerpt("출시일을 9월 5일로 변경합니다.")
                .build();

        QaResponseDto.FeedAnswer answer = QaResponseDto.FeedAnswer.builder()
                .answerId(201L)
                .content("출시일은 9월 5일입니다.")
                .answerType(AnswerType.AI)
                .revised(false)
                .createdAt(createdAt.plusSeconds(5))
                .updatedAt(createdAt.plusSeconds(5))
                .sources(List.of(source))
                .build();

        QaResponseDto.FeedQuestion question = QaResponseDto.FeedQuestion.builder()
                .questionId(101L)
                .questionerId(1L)
                .questionerName("김유진")
                .content("출시일은 언제인가요?")
                .createdAt(createdAt)
                .build();

        QaResponseDto.FeedItem item = QaResponseDto.FeedItem.builder()
                .question(question)
                .status(com._penLearning.Noddi.domain.qa.dto.QaResponseStatus.ANSWERED)
                .answer(answer)
                .build();

        return QaResponseDto.Feed.builder()
                .projectId(100L)
                .projectName("노디프로젝트")
                .teamId(10L)
                .teamName("마케팅팀")
                .items(List.of(item))
                .nextCursor(null)
                .hasNext(false)
                .build();
    }
}
