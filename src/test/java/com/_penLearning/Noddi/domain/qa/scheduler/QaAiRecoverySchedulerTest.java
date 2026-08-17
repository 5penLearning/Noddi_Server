package com._penLearning.Noddi.domain.qa.scheduler;

import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.event.QaQuestionCreatedEvent;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.qa.service.QaAiAnswerLifecycleService;
import com._penLearning.Noddi.domain.qa.service.QaAiRecoveryOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAiRecoverySchedulerTest {

    @Mock
    private QaQuestionRepository questionRepository;

    @Mock
    private QaAiAnswerLifecycleService lifecycleService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private QaQuestion question;

    @Mock
    private QaQuestion anotherQuestion;

    @Test
    void republishesEventOnlyForRecoverableQuestion() {
        QaAiRecoveryScheduler scheduler = new QaAiRecoveryScheduler(
                questionRepository,
                lifecycleService,
                eventPublisher
        );
        ReflectionTestUtils.setField(scheduler, "processingTimeoutMinutes", 10L);

        when(question.getQuestionId()).thenReturn(7L);
        when(questionRepository.findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                org.mockito.ArgumentMatchers.<Collection<QaStatus>>any(),
                any(LocalDateTime.class)
        )).thenReturn(List.of(question));
        when(lifecycleService.prepareRecovery(eq(7L), any(LocalDateTime.class)))
                .thenReturn(QaAiRecoveryOutcome.RETRY);

        scheduler.retryStalledAnswers();

        ArgumentCaptor<QaQuestionCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(QaQuestionCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().questionId()).isEqualTo(7L);
    }

    @Test
    void doesNotRepublishWhenRetryLimitWasReached() {
        QaAiRecoveryScheduler scheduler = new QaAiRecoveryScheduler(
                questionRepository,
                lifecycleService,
                eventPublisher
        );
        ReflectionTestUtils.setField(scheduler, "processingTimeoutMinutes", 10L);

        when(question.getQuestionId()).thenReturn(7L);
        when(questionRepository.findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                org.mockito.ArgumentMatchers.<Collection<QaStatus>>any(),
                any(LocalDateTime.class)
        )).thenReturn(List.of(question));
        when(lifecycleService.prepareRecovery(eq(7L), any(LocalDateTime.class)))
                .thenReturn(QaAiRecoveryOutcome.NONE);

        scheduler.retryStalledAnswers();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void continuesRecoveringOtherQuestionsWhenOneCandidateFails() {
        QaAiRecoveryScheduler scheduler = new QaAiRecoveryScheduler(
                questionRepository,
                lifecycleService,
                eventPublisher
        );
        ReflectionTestUtils.setField(scheduler, "processingTimeoutMinutes", 10L);

        when(question.getQuestionId()).thenReturn(7L);
        when(anotherQuestion.getQuestionId()).thenReturn(8L);
        when(questionRepository.findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                org.mockito.ArgumentMatchers.<Collection<QaStatus>>any(),
                any(LocalDateTime.class)
        )).thenReturn(List.of(question, anotherQuestion));
        when(lifecycleService.prepareRecovery(eq(7L), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("lock timeout"));
        when(lifecycleService.prepareRecovery(eq(8L), any(LocalDateTime.class)))
                .thenReturn(QaAiRecoveryOutcome.RETRY);

        scheduler.retryStalledAnswers();

        ArgumentCaptor<QaQuestionCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(QaQuestionCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().questionId()).isEqualTo(8L);
    }
}
