package com._penLearning.Noddi.domain.qa.event;

import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagGeneration;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.qa.scheduler.QaAiRetryScheduler;
import com._penLearning.Noddi.domain.qa.service.QaAiAnswerLifecycleService;
import com._penLearning.Noddi.domain.qa.service.QaAiFailureOutcome;
import com._penLearning.Noddi.domain.qa.service.QaAnswerStreamService;
import com._penLearning.Noddi.domain.team.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaQuestionCreatedEventHandlerTest {

    @Mock
    private QaQuestionRepository qaQuestionRepository;

    @Mock
    private QaAiAnswerLifecycleService answerLifecycleService;

    @Mock
    private QaRagAnswerGenerator answerGenerator;

    @Mock
    private QaAnswerStreamService answerStreamService;

    @Mock
    private QaAiRetryScheduler retryScheduler;

    @Mock
    private QaQuestion question;

    @Mock
    private Team team;

    @Test
    void combinesStreamChunksAndSavesCompletedAnswer() {
        // Given: AI가 두 개의 텍스트 조각을 순서대로 생성한다.
        QaQuestionCreatedEventHandler handler = createHandler();
        when(answerLifecycleService.tryStart(1L)).thenReturn(OptionalInt.of(1));
        when(qaQuestionRepository.findByIdWithTeam(1L)).thenReturn(Optional.of(question));
        when(question.getTargetTeam()).thenReturn(team);
        when(team.getTeamId()).thenReturn(10L);
        when(question.getContent()).thenReturn("백엔드는 언제 배포하나요?");
        List<RetrievedKnowledge> sources = List.of();
        when(answerGenerator.prepare(10L, "백엔드는 언제 배포하나요?"))
                .thenReturn(Mono.just(new QaRagGeneration(
                        sources,
                        Flux.just("8월 20일에 ", "배포합니다. [근거 1]")
                )));
        when(answerLifecycleService.complete(
                1L,
                1,
                "8월 20일에 배포합니다. [근거 1]",
                sources
        )).thenReturn(201L);

        // When: 질문 생성 이벤트를 처리한다.
        handler.handle(new QaQuestionCreatedEvent(1L));

        // Then: 스트림을 초기화하고 AI가 생성한 각 조각을 순서대로 SSE 서비스에 전달한다.
        verify(answerStreamService).start(1L, 1);
        verify(answerStreamService).publishChunk(1L, 1, "8월 20일에 ");
        verify(answerStreamService).publishChunk(1L, 1, "배포합니다. [근거 1]");

        // 조각 전체를 결합한 최종 답변을 DB에 저장한 뒤 COMPLETED 이벤트를 발행한다.
        verify(answerLifecycleService).complete(1L, 1, "8월 20일에 배포합니다. [근거 1]", sources);
        verify(answerStreamService).publishCompleted(
                1L,
                201L,
                "8월 20일에 배포합니다. [근거 1]"
        );
        verify(answerLifecycleService, never()).fail(1L, 1);
        verify(answerStreamService, never()).publishFailed(1L);
    }

    @Test
    void marksQuestionAsFailedWhenGenerationFails() {
        // Given: OpenAI 답변 스트림 처리 중 예외가 발생한다.
        QaQuestionCreatedEventHandler handler = createHandler();
        when(answerLifecycleService.tryStart(1L)).thenReturn(OptionalInt.of(1));
        when(answerLifecycleService.fail(1L, 1)).thenReturn(QaAiFailureOutcome.RETRYABLE);
        when(qaQuestionRepository.findByIdWithTeam(1L)).thenReturn(Optional.of(question));
        when(question.getTargetTeam()).thenReturn(team);
        when(team.getTeamId()).thenReturn(10L);
        when(question.getContent()).thenReturn("질문");
        when(answerGenerator.prepare(10L, "질문"))
                .thenReturn(Mono.just(new QaRagGeneration(
                        List.of(),
                        Flux.error(new RuntimeException("OpenAI failure"))
                )));

        // When: 질문 생성 이벤트를 처리한다.
        handler.handle(new QaQuestionCreatedEvent(1L));

        // Then: 질문을 FAILED로 변경하고 SSE 구독자에게도 실패 이벤트를 보낸다.
        verify(answerStreamService).start(1L, 1);
        verify(answerLifecycleService).fail(1L, 1);
        verify(answerStreamService).publishRetrying(1L, 1);
        verify(retryScheduler).scheduleRetry(1L, 1);
        verify(answerLifecycleService, never()).complete(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList()
        );
        verify(answerStreamService, never()).publishCompleted(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void ignoresDuplicatedEventWhenGenerationCannotStart() {
        // Given: 동일 질문의 AI 작업이 이미 실행 중이거나 완료되어 tryStart가 false를 반환한다.
        QaQuestionCreatedEventHandler handler = createHandler();
        when(answerLifecycleService.tryStart(1L)).thenReturn(OptionalInt.empty());

        handler.handle(new QaQuestionCreatedEvent(1L));

        // Then: RAG 호출뿐 아니라 SSE 세션도 중복으로 시작하지 않는다.
        verifyNoInteractions(qaQuestionRepository, answerGenerator);
        verifyNoInteractions(answerStreamService);
        verify(answerLifecycleService, never()).complete(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList()
        );
        verify(answerLifecycleService, never()).fail(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    private QaQuestionCreatedEventHandler createHandler() {
        return new QaQuestionCreatedEventHandler(
                qaQuestionRepository,
                answerLifecycleService,
                answerGenerator,
                answerStreamService,
                retryScheduler
        );
    }
}
