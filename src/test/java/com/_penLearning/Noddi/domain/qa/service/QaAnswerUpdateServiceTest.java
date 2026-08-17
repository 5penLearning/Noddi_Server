package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishType;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAnswerUpdateServiceTest {

    @Mock private QaAnswerRepository answerRepository;
    @Mock private QaAnswerRevisionRepository answerRevisionRepository;
    @Mock private QaQuestionRepository questionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private QaAnswer answer;
    @Mock private QaAnswerRevision lastRevision;
    @Mock private QaQuestion question;
    @Mock private Team team;
    @Mock private User reviser;
    @Mock private User questioner;
    @Mock private QaRequestDto.ReviseAnswer request;

    @Test
    void savesTeamAnswerAsFirstHumanRevisionAfterFinalAiFailure() {
        QaAnswerUpdateService service = service();
        stubAuthorizedRequest(QaStatus.MANUAL_REQUIRED, "팀원이 제공한 답변");
        stubEventPayload();

        service.revise(1L, 2L, request);

        ArgumentCaptor<QaAnswerRevision> revisionCaptor = ArgumentCaptor.forClass(QaAnswerRevision.class);
        verify(answerRevisionRepository).save(revisionCaptor.capture());
        QaAnswerRevision revision = revisionCaptor.getValue();
        assertThat(revision.getVersionNumber()).isEqualTo(1);
        assertThat(revision.getContent()).isEqualTo("팀원이 제공한 답변");
        assertThat(revision.getRevisedBy()).isSameAs(reviser);
        verify(answer).provideByTeam("팀원이 제공한 답변");
        verify(question).completeManualAnswer();
        assertPublishedEvent(QaAnswerPublishType.TEAM_PROVIDED);
    }

    @Test
    void savesNextRevisionAndUpdatesLatestAnswer() {
        QaAnswerUpdateService service = service();
        stubAuthorizedRequest(QaStatus.ANSWERED, "담당자가 수정한 최종 답변");
        stubEventPayload();
        when(answer.getContent()).thenReturn("기존 AI 답변");
        when(answerRevisionRepository.findTopByAnswerOrderByVersionNumberDesc(answer))
                .thenReturn(Optional.of(lastRevision));
        when(lastRevision.getVersionNumber()).thenReturn(1);

        service.revise(1L, 2L, request);

        ArgumentCaptor<QaAnswerRevision> revisionCaptor = ArgumentCaptor.forClass(QaAnswerRevision.class);
        verify(answerRevisionRepository).save(revisionCaptor.capture());
        QaAnswerRevision revision = revisionCaptor.getValue();
        assertThat(revision.getVersionNumber()).isEqualTo(2);
        assertThat(revision.getContent()).isEqualTo("담당자가 수정한 최종 답변");
        assertThat(revision.getRevisedBy()).isSameAs(reviser);
        verify(answer).revise("담당자가 수정한 최종 답변", reviser);
        assertPublishedEvent(QaAnswerPublishType.ANSWER_REVISED);
    }

    @Test
    void doesNothingWhenAnsweredContentIsIdentical() {
        QaAnswerUpdateService service = service();
        stubAuthorizedRequest(QaStatus.ANSWERED, "동일한 답변");
        when(answer.getContent()).thenReturn("동일한 답변");

        service.revise(1L, 2L, request);

        verify(answerRevisionRepository, never()).save(any());
        verify(answer, never()).revise(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsRequesterWhoIsNotTargetTeamMember() {
        QaAnswerUpdateService service = service();
        when(answerRepository.findByIdWithQuestionAndTeamForUpdate(1L)).thenReturn(Optional.of(answer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviser));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getTargetTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, reviser)).thenReturn(false);

        assertThatThrownBy(() -> service.revise(1L, 2L, request))
                .isInstanceOf(com._penLearning.Noddi.global.exception.GeneralException.class);

        verify(answerRevisionRepository, never()).save(any());
        verify(answer, never()).revise(any(), any());
    }

    private QaAnswerUpdateService service() {
        return new QaAnswerUpdateService(
                answerRepository,
                answerRevisionRepository,
                questionRepository,
                userRepository,
                teamMemberRepository,
                eventPublisher
        );
    }

    private void stubAuthorizedRequest(QaStatus status, String content) {
        when(answerRepository.findByIdWithQuestionAndTeamForUpdate(1L)).thenReturn(Optional.of(answer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviser));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getQuestionId()).thenReturn(10L);
        when(question.getTargetTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, reviser)).thenReturn(true);
        when(questionRepository.findByIdWithLock(10L)).thenReturn(Optional.of(question));
        when(question.getStatus()).thenReturn(status);
        when(request.getContent()).thenReturn(content);
    }

    private void stubEventPayload() {
        when(question.getQuestioner()).thenReturn(questioner);
        when(questioner.getUserId()).thenReturn(20L);
        when(team.getTeamId()).thenReturn(30L);
        when(answer.getAnswerId()).thenReturn(1L);
        when(reviser.getUserId()).thenReturn(2L);
    }

    private void assertPublishedEvent(QaAnswerPublishType publishType) {
        ArgumentCaptor<QaAnswerPublishedEvent> eventCaptor = ArgumentCaptor.forClass(QaAnswerPublishedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new QaAnswerPublishedEvent(
                10L,
                1L,
                20L,
                30L,
                2L,
                publishType
        ));
    }
}
