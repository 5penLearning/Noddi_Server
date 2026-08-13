package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAiAnswerLifecycleServiceTest {

    @Mock
    private QaQuestionRepository qaQuestionRepository;

    @Mock
    private QaAnswerRepository qaAnswerRepository;

    @Mock
    private QaAnswerSourceRepository qaAnswerSourceRepository;

    @Mock
    private QaQuestion question;

    @Mock
    private QaAnswer answer;

    @Test
    void savesAnswerAndRetrievedSourcesTogether() {
        QaAiAnswerLifecycleService service = new QaAiAnswerLifecycleService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository
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

        Long answerId = service.complete(1L, "최종 답변", sources);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QaAnswerSource>> sourceCaptor = ArgumentCaptor.forClass(List.class);
        verify(qaAnswerSourceRepository).saveAll(sourceCaptor.capture());

        assertThat(answerId).isEqualTo(100L);
        assertThat(sourceCaptor.getValue())
                .extracting(
                        QaAnswerSource::getSourceType,
                        QaAnswerSource::getReferenceId,
                        QaAnswerSource::getSourceTitle,
                        QaAnswerSource::getExcerpt
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                SourceType.TRANSCRIPT,
                                20L,
                                "백엔드 배포 회의",
                                "배포일은 8월 20일입니다."
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                SourceType.TEAM_TEXT,
                                30L,
                                "배포 체크리스트",
                                "배포 전에 API 테스트를 완료합니다."
                        )
                );
        verify(question).markAsAnswered();
    }
}
