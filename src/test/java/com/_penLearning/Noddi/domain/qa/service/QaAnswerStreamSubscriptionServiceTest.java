package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAnswerStreamSubscriptionServiceTest {

    /*
     * 이 테스트는 SSE의 실제 텍스트 전송보다 구독 직전의 DB 조회와 권한 판단을 검증한다.
     *
     * 1. 로그인 사용자와 질문을 조회한다.
     * 2. 사용자가 질문 대상 팀과 같은 프로젝트의 멤버인지 확인한다.
     * 3. 질문 상태에 따라 저장된 답변을 조회한다.
     * 4. 스트림 서비스에 현재 상태를 정확하게 전달한다.
     */

    @Mock
    private QaQuestionRepository qaQuestionRepository;

    @Mock
    private QaAnswerRepository qaAnswerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private QaAnswerStreamService qaAnswerStreamService;

    @Mock
    private User requester;

    @Mock
    private QaQuestion question;

    @Mock
    private Team targetTeam;

    @Mock
    private Project project;

    private QaAnswerStreamSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new QaAnswerStreamSubscriptionService(
                qaQuestionRepository,
                qaAnswerRepository,
                userRepository,
                projectMemberRepository,
                qaAnswerStreamService
        );
    }

    @Test
    void subscribesToProcessingQuestionWithoutStoredAnswer() {
        // Given: 프로젝트 멤버가 아직 AI 답변을 생성 중인 질문을 구독한다.
        allowProjectAccess();
        when(question.getQuestionId()).thenReturn(101L);
        when(question.getStatus()).thenReturn(QaStatus.PROCESSING);

        SseEmitter expectedEmitter = mock(SseEmitter.class);
        when(qaAnswerStreamService.subscribe(
                101L,
                QaStatus.PROCESSING,
                null,
                null
        )).thenReturn(expectedEmitter);

        // When: 답변 스트림 구독을 요청한다.
        SseEmitter actualEmitter = service.subscribe(1L, 101L);

        // Then: 생성 중에는 아직 DB에 QaAnswer가 없으므로 답변을 조회하지 않고
        // answerId와 answerContent를 null로 전달해 실시간 연결만 생성한다.
        assertThat(actualEmitter).isSameAs(expectedEmitter);
        verify(qaAnswerStreamService).subscribe(
                101L,
                QaStatus.PROCESSING,
                null,
                null
        );
        verifyNoInteractions(qaAnswerRepository);
    }

    @Test
    void returnsStoredAnswerWhenQuestionIsAlreadyAnswered() {
        // Given: 프로젝트 멤버가 이미 답변 생성과 DB 저장이 끝난 질문을 구독한다.
        allowProjectAccess();
        when(question.getQuestionId()).thenReturn(101L);
        when(question.getStatus()).thenReturn(QaStatus.ANSWERED);

        QaAnswer answer = mock(QaAnswer.class);
        when(qaAnswerRepository.findByQuestion(question)).thenReturn(Optional.of(answer));
        when(answer.getAnswerId()).thenReturn(201L);
        when(answer.getContent()).thenReturn("출시일은 9월 5일입니다.");

        SseEmitter expectedEmitter = mock(SseEmitter.class);
        when(qaAnswerStreamService.subscribe(
                101L,
                QaStatus.ANSWERED,
                201L,
                "출시일은 9월 5일입니다."
        )).thenReturn(expectedEmitter);

        // When: 완료된 질문의 스트림을 구독한다.
        SseEmitter actualEmitter = service.subscribe(1L, 101L);

        // Then: 메모리 스트림 유무와 관계없이 DB의 최종 답변을 COMPLETED 이벤트에
        // 사용할 수 있도록 스트림 서비스에 함께 전달한다.
        assertThat(actualEmitter).isSameAs(expectedEmitter);
        verify(qaAnswerStreamService).subscribe(
                101L,
                QaStatus.ANSWERED,
                201L,
                "출시일은 9월 5일입니다."
        );
    }

    @Test
    void rejectsRequesterWhoIsNotProjectMember() {
        // Given: 사용자와 질문은 존재하지만 사용자가 해당 프로젝트의 멤버가 아니다.
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(qaQuestionRepository.findByIdWithTeam(101L)).thenReturn(Optional.of(question));
        when(question.getTargetTeam()).thenReturn(targetTeam);
        when(targetTeam.getProject()).thenReturn(project);
        when(projectMemberRepository.existsByProjectAndUser(project, requester))
                .thenReturn(false);

        // When & Then: 프로젝트 외부 사용자의 구독을 예외로 거절한다.
        assertThatThrownBy(() -> service.subscribe(1L, 101L))
                .isInstanceOf(GeneralException.class);

        // 권한 검증에 실패하면 DB 답변 조회나 SSE 연결 생성까지 진행하지 않는다.
        verifyNoInteractions(qaAnswerRepository, qaAnswerStreamService);
    }

    @Test
    void rejectsMissingQuestionBeforeOpeningStream() {
        // Given: 로그인 사용자는 존재하지만 요청한 질문 ID가 존재하지 않는다.
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(qaQuestionRepository.findByIdWithTeam(999L)).thenReturn(Optional.empty());

        // When & Then: 존재하지 않는 질문의 SSE 연결을 만들지 않고 예외를 반환한다.
        assertThatThrownBy(() -> service.subscribe(1L, 999L))
                .isInstanceOf(GeneralException.class);

        verifyNoInteractions(
                projectMemberRepository,
                qaAnswerRepository,
                qaAnswerStreamService
        );
    }

    private void allowProjectAccess() {
        // 성공 테스트에서 반복되는 "요청자가 질문의 프로젝트 멤버인 상황"을 준비한다.
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(qaQuestionRepository.findByIdWithTeam(101L)).thenReturn(Optional.of(question));
        when(question.getTargetTeam()).thenReturn(targetTeam);
        when(targetTeam.getProject()).thenReturn(project);
        when(projectMemberRepository.existsByProjectAndUser(project, requester))
                .thenReturn(true);
    }
}
