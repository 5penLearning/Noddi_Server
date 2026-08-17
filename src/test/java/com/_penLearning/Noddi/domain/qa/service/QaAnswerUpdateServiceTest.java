package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAnswerUpdateServiceTest {

    @Mock private QaAnswerRepository answerRepository;
    @Mock private QaAnswerRevisionRepository answerRevisionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private QaAnswer answer;
    @Mock private QaAnswerRevision lastRevision;
    @Mock private QaQuestion question;
    @Mock private Team team;
    @Mock private User reviser;
    @Mock private QaRequestDto.ReviseAnswer request;

    @Test
    void savesNextRevisionAndUpdatesLatestAnswer() {
        QaAnswerUpdateService service = new QaAnswerUpdateService(
                answerRepository,
                answerRevisionRepository,
                userRepository,
                teamMemberRepository
        );

        // Given: AI 최초 답변인 v1이 존재하고 대상 팀 담당자가 답변을 수정한다.
        when(answerRepository.findByIdWithQuestionAndTeamForUpdate(1L)).thenReturn(Optional.of(answer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviser));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getTargetTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, reviser)).thenReturn(true);
        when(answerRevisionRepository.findTopByAnswerOrderByVersionNumberDesc(answer))
                .thenReturn(Optional.of(lastRevision));
        when(lastRevision.getVersionNumber()).thenReturn(1);
        when(request.getContent()).thenReturn("담당자가 수정한 최종 답변");

        // When: 담당자가 답변 수정을 요청한다.
        service.revise(1L, 2L, request);

        // Then: 기존 AI v1은 변경하지 않고 담당자 수정 내용을 v2로 새로 저장한다.
        org.mockito.ArgumentCaptor<QaAnswerRevision> revisionCaptor =
                org.mockito.ArgumentCaptor.forClass(QaAnswerRevision.class);
        verify(answerRevisionRepository).save(revisionCaptor.capture());

        QaAnswerRevision savedRevision = revisionCaptor.getValue();
        assertThat(savedRevision.getAnswer()).isSameAs(answer);
        assertThat(savedRevision.getVersionNumber()).isEqualTo(2);
        assertThat(savedRevision.getContent()).isEqualTo("담당자가 수정한 최종 답변");
        assertThat(savedRevision.getEditorType().name()).isEqualTo("HUMAN");
        assertThat(savedRevision.getRevisedBy()).isSameAs(reviser);

        // 피드에서 바로 보여줄 QaAnswer도 같은 내용과 마지막 수정자로 갱신한다.
        verify(answer).revise("담당자가 수정한 최종 답변", reviser);
    }

    @Test
    void doesNotSaveRevisionWhenRequesterIsNotTargetTeamMember() {
        QaAnswerUpdateService service = new QaAnswerUpdateService(
                answerRepository,
                answerRevisionRepository,
                userRepository,
                teamMemberRepository
        );

        // Given: 답변과 사용자는 존재하지만 사용자가 질문 대상 팀 소속이 아니다.
        when(answerRepository.findByIdWithQuestionAndTeamForUpdate(1L)).thenReturn(Optional.of(answer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviser));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getTargetTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, reviser)).thenReturn(false);

        // When: 권한 없는 사용자가 답변을 수정하려고 한다.
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.revise(1L, 2L, request)
                )
                .isInstanceOf(com._penLearning.Noddi.global.exception.GeneralException.class);

        // Then: 새 수정 이력과 최신 답변 모두 변경되지 않는다.
        verify(answerRevisionRepository, never()).save(any(QaAnswerRevision.class));
        verify(answer, never()).revise(any(), any());
    }
}
