package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishType;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAnswerUpdateServiceTest {

    @Mock private QaAnswerRepository answerRepository;
    @Mock private QaAnswerSourceRepository answerSourceRepository;
    @Mock private QaQuestionRepository questionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private QaAnswer answer;
    @Mock private QaQuestion question;
    @Mock private Team team;
    @Mock private User reviser;
    @Mock private User questioner;
    @Mock private QaRequestDto.ReviseAnswer request;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void removesAiSourcesWhenAnswerIsRevised() {
        QaAnswerUpdateService service = new QaAnswerUpdateService(
                answerRepository,
                answerSourceRepository,
                questionRepository,
                userRepository,
                teamMemberRepository,
                eventPublisher
        );

        when(answerRepository.findByIdWithQuestionAndTeamForUpdate(1L)).thenReturn(Optional.of(answer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviser));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getTargetTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, reviser)).thenReturn(true);
        when(request.getContent()).thenReturn("담당자가 수정한 최종 답변");
        when(answer.getAnswerId()).thenReturn(1L);

        service.revise(1L, 2L, request);

        verify(answer).revise("담당자가 수정한 최종 답변", reviser);
        verify(answerSourceRepository).deleteAllByAnswerId(1L);
    }
}
