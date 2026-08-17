package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.event.QaAiFinalFailureEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishType;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QaAiAnswerLifecycleServiceTest {

    @Mock
    private QaQuestionRepository qaQuestionRepository;

    @Mock
    private QaAnswerRepository qaAnswerRepository;

    @Mock
    private QaAnswerSourceRepository qaAnswerSourceRepository;

    @Mock
    private QaAnswerRevisionRepository qaAnswerRevisionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private QaQuestion question;

    @Mock
    private QaAnswer answer;

    @Mock
    private User questioner;

    @Mock
    private Team targetTeam;

    @Test
    void savesAnswerAndRetrievedSourcesTogether() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        List<RetrievedKnowledge> sources = List.of(
                new RetrievedKnowledge(
                        "knowledge-transcript-20-0",
                        20L,
                        SourceType.TRANSCRIPT,
                        "백엔드 배포 회의",
                        "배포일은 8월 20일입니다.",
                        0,
                        0.529
                ),
                new RetrievedKnowledge(
                        "knowledge-team-text-30-0",
                        30L,
                        SourceType.TEAM_TEXT,
                        "배포 체크리스트",
                        "배포 전에 API 테스트를 완료합니다.",
                        0,
                        0.412
                )
        );

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);
        when(question.isCurrentAttempt(1)).thenReturn(true);
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);
        when(qaAnswerRepository.save(any(QaAnswer.class))).thenReturn(answer);
        when(answer.getAnswerId()).thenReturn(100L);
        when(answer.getContent()).thenReturn("첫 번째 자료만 사용한 답변입니다. [근거 1]");
        stubAnswerPublishedEventPayload();

        Long answerId = service.complete(1L, 1, "첫 번째 자료만 사용한 답변입니다. [근거 1]", sources);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QaAnswerSource>> sourceCaptor = ArgumentCaptor.forClass(List.class);
        verify(qaAnswerSourceRepository).saveAll(sourceCaptor.capture());

        assertThat(answerId).isEqualTo(100L);
        assertThat(sourceCaptor.getValue())
                .extracting(
                        QaAnswerSource::getSourceType,
                        QaAnswerSource::getCitationIndex,
                        QaAnswerSource::getReferenceId,
                        QaAnswerSource::getSourceTitle,
                        QaAnswerSource::getExcerpt
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                SourceType.TRANSCRIPT,
                                1,
                                20L,
                                "백엔드 배포 회의",
                                "배포일은 8월 20일입니다."
                        )
                );
        verify(question).markAsAnswered();
        ArgumentCaptor<QaAnswerRevision> revisionCaptor = ArgumentCaptor.forClass(QaAnswerRevision.class);
        verify(qaAnswerRevisionRepository).save(revisionCaptor.capture());
        assertThat(revisionCaptor.getValue().getVersionNumber()).isEqualTo(1);
        assertThat(revisionCaptor.getValue().getContent())
                .isEqualTo("첫 번째 자료만 사용한 답변입니다. [근거 1]");
        ArgumentCaptor<QaAnswerPublishedEvent> eventCaptor =
                ArgumentCaptor.forClass(QaAnswerPublishedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new QaAnswerPublishedEvent(
                1L,
                100L,
                10L,
                20L,
                null,
                QaAnswerPublishType.AI_GENERATED
        ));
    }

    @Test
    void rejectsAnswerWithoutValidCitationWhenKnowledgeWasRetrieved() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        List<RetrievedKnowledge> sources = List.of(new RetrievedKnowledge(
                "knowledge-transcript-20-0",
                20L,
                SourceType.TRANSCRIPT,
                "배포 회의",
                "배포일은 8월 20일입니다.",
                0,
                0.8
        ));

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);
        when(question.isCurrentAttempt(1)).thenReturn(true);
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);

        assertThatThrownBy(() -> service.complete(1L, 1, "8월 20일에 배포합니다.", sources))
                .isInstanceOf(com._penLearning.Noddi.global.exception.GeneralException.class);

        verify(qaAnswerRepository, never()).save(any(QaAnswer.class));
        verify(question, never()).markAsAnswered();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void permitsFixedInsufficientEvidenceAnswerWithoutCitation() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        List<RetrievedKnowledge> sources = List.of(new RetrievedKnowledge(
                "knowledge-transcript-20-0",
                20L,
                SourceType.TRANSCRIPT,
                "배포 회의",
                "배포일은 8월 20일입니다.",
                0,
                0.8
        ));

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);
        when(question.isCurrentAttempt(1)).thenReturn(true);
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);
        when(qaAnswerRepository.save(any(QaAnswer.class))).thenReturn(answer);
        when(answer.getAnswerId()).thenReturn(100L);
        when(answer.getContent()).thenReturn(
                com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator.INSUFFICIENT_EVIDENCE_MESSAGE
        );
        stubAnswerPublishedEventPayload();

        Long answerId = service.complete(
                1L,
                1,
                com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator.INSUFFICIENT_EVIDENCE_MESSAGE,
                sources
        );

        assertThat(answerId).isEqualTo(100L);
        verify(qaAnswerSourceRepository).saveAll(List.of());
        verify(question).markAsAnswered();
    }

    @Test
    void turnsStaleProcessingQuestionIntoRetryableFailure() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getUpdatedAt()).thenReturn(threshold.minusSeconds(1));
        when(question.canRetry(3)).thenReturn(true);
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);

        assertThat(service.prepareRecovery(1L, threshold)).isEqualTo(QaAiRecoveryOutcome.RETRY);
        verify(question).markAsFailed();
    }

    @Test
    void doesNotRecoverRecentlyUpdatedQuestion() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getUpdatedAt()).thenReturn(threshold.plusSeconds(1));

        assertThat(service.prepareRecovery(1L, threshold)).isEqualTo(QaAiRecoveryOutcome.NONE);
        verify(question, never()).markAsFailed();
    }

    @Test
    void createsSystemNoticeAndPublishesEventAfterThirdFailure() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        com._penLearning.Noddi.domain.user.entity.User questioner = mock(
                com._penLearning.Noddi.domain.user.entity.User.class
        );
        com._penLearning.Noddi.domain.team.entity.Team targetTeam = mock(
                com._penLearning.Noddi.domain.team.entity.Team.class
        );

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);
        when(question.isCurrentAttempt(3)).thenReturn(true);
        when(question.canRetry(3)).thenReturn(false);
        when(question.getQuestionId()).thenReturn(1L);
        when(question.getQuestioner()).thenReturn(questioner);
        when(question.getTargetTeam()).thenReturn(targetTeam);
        when(questioner.getUserId()).thenReturn(10L);
        when(targetTeam.getTeamId()).thenReturn(20L);
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);
        when(qaAnswerRepository.save(any(QaAnswer.class))).thenAnswer(invocation -> {
            QaAnswer saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "answerId", 30L);
            return saved;
        });

        QaAiFailureOutcome outcome = service.fail(1L, 3);

        assertThat(outcome).isEqualTo(QaAiFailureOutcome.FINALIZED);
        verify(question).markAsManualRequired();
        ArgumentCaptor<QaAiFinalFailureEvent> eventCaptor =
                ArgumentCaptor.forClass(QaAiFinalFailureEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().answerId()).isEqualTo(30L);
        assertThat(eventCaptor.getValue().noticeContent())
                .isEqualTo(QaAiAnswerLifecycleService.MANUAL_ANSWER_NOTICE);
        verifyNoInteractions(qaAnswerRevisionRepository);
    }

    @Test
    void ignoresFailureFromOlderAttempt() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository,
                eventPublisher
        );
        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);
        when(question.isCurrentAttempt(1)).thenReturn(false);

        assertThat(service.fail(1L, 1)).isEqualTo(QaAiFailureOutcome.IGNORED);

        verify(question, never()).markAsFailed();
        verify(question, never()).markAsManualRequired();
        verifyNoInteractions(qaAnswerRepository, eventPublisher);
    }

    private void stubAnswerPublishedEventPayload() {
        when(question.getQuestionId()).thenReturn(1L);
        when(question.getQuestioner()).thenReturn(questioner);
        when(question.getTargetTeam()).thenReturn(targetTeam);
        when(questioner.getUserId()).thenReturn(10L);
        when(targetTeam.getTeamId()).thenReturn(20L);
    }
}
