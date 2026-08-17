package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

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
    private QaQuestion question;

    @Mock
    private QaAnswer answer;

    @Test
    void savesAnswerAndRetrievedSourcesTogether() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository
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
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);
        when(qaAnswerRepository.save(any(QaAnswer.class))).thenReturn(answer);
        when(answer.getAnswerId()).thenReturn(100L);
        when(answer.getContent()).thenReturn("첫 번째 자료만 사용한 답변입니다. [근거 1]");

        Long answerId = service.complete(1L, "첫 번째 자료만 사용한 답변입니다. [근거 1]", sources);

        // AI가 만든 답변은 현재 답변(QaAnswer)으로만 남는 것이 아니라,
        // 담당자가 나중에 수정해도 원문을 볼 수 있도록 수정 이력 v1에도 저장되어야 한다.
        ArgumentCaptor<QaAnswerRevision> revisionCaptor =
                ArgumentCaptor.forClass(QaAnswerRevision.class);
        verify(qaAnswerRevisionRepository).save(revisionCaptor.capture());

        QaAnswerRevision initialRevision = revisionCaptor.getValue();
        assertThat(initialRevision.getAnswer()).isSameAs(answer);
        assertThat(initialRevision.getVersionNumber()).isEqualTo(1);
        assertThat(initialRevision.getContent())
                .isEqualTo("첫 번째 자료만 사용한 답변입니다. [근거 1]");
        assertThat(initialRevision.getEditorType().name()).isEqualTo("AI");
        assertThat(initialRevision.getRevisedBy()).isNull();

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
    }

    @Test
    void rejectsAnswerWithoutValidCitationWhenKnowledgeWasRetrieved() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository
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
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);

        assertThatThrownBy(() -> service.complete(1L, "8월 20일에 배포합니다.", sources))
                .isInstanceOf(com._penLearning.Noddi.global.exception.GeneralException.class);

        verify(qaAnswerRepository, never()).save(any(QaAnswer.class));
        verify(qaAnswerRevisionRepository, never()).save(any(QaAnswerRevision.class));
        verify(question, never()).markAsAnswered();
    }

    @Test
    void permitsFixedInsufficientEvidenceAnswerWithoutCitation() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository
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
        when(qaAnswerRepository.existsByQuestion(question)).thenReturn(false);
        when(qaAnswerRepository.save(any(QaAnswer.class))).thenReturn(answer);
        when(answer.getAnswerId()).thenReturn(100L);

        Long answerId = service.complete(
                1L,
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
                qaAnswerRevisionRepository
        );
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getUpdatedAt()).thenReturn(threshold.minusSeconds(1));
        when(question.canRetry(3)).thenReturn(true);
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING, QaStatus.FAILED);

        assertThat(service.prepareRecovery(1L, threshold)).isTrue();
        verify(question).markAsFailed();
    }

    @Test
    void doesNotRecoverRecentlyUpdatedQuestion() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                qaAnswerRevisionRepository
        );
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        when(qaQuestionRepository.findByIdWithLock(1L)).thenReturn(Optional.of(question));
        when(question.getUpdatedAt()).thenReturn(threshold.plusSeconds(1));

        assertThat(service.prepareRecovery(1L, threshold)).isFalse();
        verify(question, never()).markAsFailed();
    }
}
