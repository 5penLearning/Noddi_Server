package com._penLearning.Noddi.domain.qa.event;

import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagGeneration;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.qa.service.QaAiAnswerLifecycleService;
import com._penLearning.Noddi.domain.team.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

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
    private QaQuestion question;

    @Mock
    private Team team;

    @Test
    void combinesStreamChunksAndSavesCompletedAnswer() {
        QaQuestionCreatedEventHandler handler = createHandler();
        when(answerLifecycleService.tryStart(1L)).thenReturn(true);
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

        handler.handle(new QaQuestionCreatedEvent(1L));

        verify(answerLifecycleService).complete(1L, "8월 20일에 배포합니다. [근거 1]", sources);
        verify(answerLifecycleService, never()).fail(1L);
    }

    @Test
    void marksQuestionAsFailedWhenGenerationFails() {
        QaQuestionCreatedEventHandler handler = createHandler();
        when(answerLifecycleService.tryStart(1L)).thenReturn(true);
        when(qaQuestionRepository.findByIdWithTeam(1L)).thenReturn(Optional.of(question));
        when(question.getTargetTeam()).thenReturn(team);
        when(team.getTeamId()).thenReturn(10L);
        when(question.getContent()).thenReturn("질문");
        when(answerGenerator.prepare(10L, "질문"))
                .thenReturn(Mono.just(new QaRagGeneration(
                        List.of(),
                        Flux.error(new RuntimeException("OpenAI failure"))
                )));

        handler.handle(new QaQuestionCreatedEvent(1L));

        verify(answerLifecycleService).fail(1L);
        verify(answerLifecycleService, never()).complete(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void ignoresDuplicatedEventWhenGenerationCannotStart() {
        QaQuestionCreatedEventHandler handler = createHandler();
        when(answerLifecycleService.tryStart(1L)).thenReturn(false);

        handler.handle(new QaQuestionCreatedEvent(1L));

        verifyNoInteractions(qaQuestionRepository, answerGenerator);
        verify(answerLifecycleService, never()).complete(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList()
        );
        verify(answerLifecycleService, never()).fail(org.mockito.ArgumentMatchers.anyLong());
    }

    private QaQuestionCreatedEventHandler createHandler() {
        return new QaQuestionCreatedEventHandler(
                qaQuestionRepository,
                answerLifecycleService,
                answerGenerator
        );
    }
}
