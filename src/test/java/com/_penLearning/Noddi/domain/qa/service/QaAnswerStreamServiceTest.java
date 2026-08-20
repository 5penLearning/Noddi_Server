package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QaAnswerStreamServiceTest {

    /*
     * 이 테스트는 Mock Repository를 사용하는 일반 서비스 단위 테스트와 달리,
     * QaAnswerStreamService가 만든 SseEmitter를 간단한 테스트 컨트롤러에 연결한다.
     *
     * 이렇게 하면 DTO 메서드 호출 여부만 확인하는 대신 브라우저가 실제로 받게 될
     * event: ... / data: ... 형식과 이벤트 순서까지 검증할 수 있다.
     */

    private QaAnswerStreamService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new QaAnswerStreamService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestStreamController(service))
                .build();
    }

    @Test
    void sendsConnectedChunksAndCompletedEventInOrder() throws Exception {
        // Given: AI 답변 생성이 시작된 질문을 사용자가 먼저 구독한다.
        service.start(101L, 1);
        MvcResult subscription = openProcessingStream();

        // When: AI가 두 조각을 생성하고 최종 답변을 DB에 저장했다고 알린다.
        service.publishChunk(101L, 1, "출시일은 ");
        service.publishChunk(101L, 1, "9월 5일입니다.");
        service.publishCompleted(
                101L,
                201L,
                "출시일은 9월 5일입니다."
        );

        // COMPLETED가 emitter를 닫은 후 비동기 결과를 최종 응답으로 가져온다.
        mockMvc.perform(asyncDispatch(subscription))
                .andExpect(status().isOk());

        // MockHttpServletResponse의 기본 문자셋은 ISO-8859-1이므로
        // 실제 JSON/SSE 응답 문자셋인 UTF-8로 명시해서 한글을 읽는다.
        String body = subscription.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Then: 연결, 두 조각, 완료 이벤트가 실제 SSE 형식으로 모두 전달된다.
        assertThat(body)
                .contains("event:connected")
                .contains("event:chunk")
                .contains("\"delta\":\"출시일은 \"")
                .contains("\"delta\":\"9월 5일입니다.\"")
                .contains("event:completed")
                .contains("\"answerId\":201")
                .contains("\"content\":\"출시일은 9월 5일입니다.\"");

        // 이벤트가 발생한 순서도 connected -> chunk -> completed여야 한다.
        assertThat(body.indexOf("event:connected"))
                .isLessThan(body.indexOf("event:chunk"));
        assertThat(body.lastIndexOf("event:chunk"))
                .isLessThan(body.indexOf("event:completed"));
    }

    @Test
    void sendsSnapshotBeforeNewChunksToLateSubscriber() throws Exception {
        // Given: 구독자가 접속하기 전에 AI가 답변 일부를 이미 생성했다.
        service.start(101L, 1);
        service.publishChunk(101L, 1, "이미 생성된 답변");

        // When: 사용자가 뒤늦게 구독한 뒤 새로운 조각과 완료 이벤트가 발생한다.
        MvcResult subscription = openProcessingStream();
        service.publishChunk(101L, 1, " 뒤의 조각");
        service.publishCompleted(
                101L,
                201L,
                "이미 생성된 답변 뒤의 조각"
        );

        mockMvc.perform(asyncDispatch(subscription))
                .andExpect(status().isOk());

        String body = subscription.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Then: 접속 전에 생성된 문자열은 snapshot 전체 내용으로 복구하고,
        // 접속 후 새로 생성된 문자열부터 delta로 이어서 전달한다.
        assertThat(body)
                .contains("event:snapshot")
                .contains("\"content\":\"이미 생성된 답변\"")
                .contains("\"delta\":\" 뒤의 조각\"")
                .contains("event:completed");

        assertThat(body.indexOf("event:snapshot"))
                .isLessThan(body.indexOf("event:chunk"));
    }

    @Test
    void ignoresLateChunkFromPreviousAttemptAfterRetryStarts() throws Exception {
        service.start(101L, 1);
        MvcResult subscription = openProcessingStream();
        service.publishChunk(101L, 1, "1차 생성 조각");

        service.publishRetrying(101L, 1);
        service.start(101L, 2);

        // 2차 시도가 시작된 뒤 도착한 1차 조각은 새 답변에 섞이면 안 된다.
        service.publishChunk(101L, 1, "뒤늦은 1차 조각");
        service.publishChunk(101L, 2, "2차 생성 조각");
        service.publishCompleted(101L, 201L, "2차 생성 조각");

        mockMvc.perform(asyncDispatch(subscription))
                .andExpect(status().isOk());

        String body = subscription.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(body)
                .contains("\"delta\":\"1차 생성 조각\"")
                .contains("event:retrying")
                .contains("\"delta\":\"2차 생성 조각\"")
                .contains("\"attempt\":2")
                .doesNotContain("뒤늦은 1차 조각");
    }

    @Test
    void sendsFailedEventAndClosesStream() throws Exception {
        // Given: 답변 생성 중인 질문을 사용자가 구독한다.
        service.start(101L, 1);
        MvcResult subscription = openProcessingStream();

        // When: AI 답변 생성이 실패한다.
        service.publishFailed(101L);

        mockMvc.perform(asyncDispatch(subscription))
                .andExpect(status().isOk());

        String body = subscription.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Then: connected 이후 FAILED 상태를 담은 실패 이벤트가 전달되고 연결이 종료된다.
        assertThat(body)
                .contains("event:connected")
                .contains("event:failed")
                .contains("\"status\":\"FAILED\"");

        assertThat(body.indexOf("event:connected"))
                .isLessThan(body.indexOf("event:failed"));
    }

    private MvcResult openProcessingStream() throws Exception {
        // SseEmitter는 응답을 즉시 끝내지 않으므로 asyncStarted 상태를 확인한다.
        return mockMvc.perform(get("/test/qa/answer-stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    /**
     * QaAnswerStreamService의 SseEmitter를 MockMvc에 연결하기 위한 테스트 전용 컨트롤러다.
     * 운영 코드의 QaController나 인증/권한 로직은 별도의 테스트에서 검증한다.
     */
    @RestController
    private static class TestStreamController {

        private final QaAnswerStreamService service;

        private TestStreamController(QaAnswerStreamService service) {
            this.service = service;
        }

        @GetMapping(
                value = "/test/qa/answer-stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE
        )
        public SseEmitter subscribe() {
            return service.subscribe(
                    101L,
                    QaStatus.PROCESSING,
                    null,
                    null
            );
        }
    }

    @Test
    void hidesFinalAiFailureAndSendsTeamAnswerPendingEvent() throws Exception {
        // Given: 질문자가 AI 답변 생성 스트림을 구독하고 있다.
        service.start(101L, 3);
        MvcResult subscription = openProcessingStream();

        // When: AI 생성이 최종 실패하여 담당 팀의 직접 답변을 기다리게 된다.
        service.publishTeamAnswerPending(
                101L,
                201L,
                "담당 팀원이 질문을 확인하고 있습니다. 답변이 등록되면 알려드리겠습니다."
        );

        mockMvc.perform(asyncDispatch(subscription))
                .andExpect(status().isOk());

        String body = subscription.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Then: 질문자에게 AI 실패 및 수동 답변 필요 상태를 노출하지 않는다.
        assertThat(body)
                .contains("event:team_answer_pending")
                .contains("\"status\":\"TEAM_ANSWER_PENDING\"")
                .contains("담당 팀원이 질문을 확인하고 있습니다.")
                .doesNotContain("event:manual_required")
                .doesNotContain("\"status\":\"MANUAL_REQUIRED\"");
    }
}
