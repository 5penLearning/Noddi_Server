package com._penLearning.Noddi.domain.notification.event;

import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCreateService;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.qa.event.QaAiFinalFailureEvent;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishType;
import com._penLearning.Noddi.domain.qa.event.QaAnswerPublishedEvent;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaNotificationEventHandlerTest {

    @Mock private NotificationCreateService notificationCreateService;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private NotificationMessageFactory messageFactory;
    @Mock private Team targetTeam;
    @Mock private Project project;
    @Mock private TeamMember firstMember;
    @Mock private TeamMember secondMember;
    @Mock private User firstUser;
    @Mock private User secondUser;

    private QaNotificationEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new QaNotificationEventHandler(
                notificationCreateService,
                teamRepository,
                teamMemberRepository,
                messageFactory
        );
    }

    @Test
    void createsQuestionerAndTargetTeamNotificationsWhenAiAnswerIsGenerated() {
        // Given: 질문 대상 팀에 두 명의 팀원이 있고 AI 답변이 생성됐다.
        stubTargetTeam();
        stubTargetTeamMembers();
        when(messageFactory.createQaMessage(
                NotificationType.QA_ANSWERED,
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("질문자 답변 등록 알림");
        when(messageFactory.createQaMessage(
                NotificationType.QA_AI_REVIEW_REQUIRED,
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("대상 팀 검토 요청 알림");

        QaAnswerPublishedEvent event = new QaAnswerPublishedEvent(
                100L,
                200L,
                10L,
                2L,
                null,
                QaAnswerPublishType.AI_GENERATED
        );

        // When: 답변 생성 완료 이벤트를 처리한다.
        handler.handleAnswerPublished(event);

        // Then: 질문자에게 답변 완료 알림을 한 건 생성한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.QA_ANSWERED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "질문자 답변 등록 알림"
        );

        // 대상 팀원 두 명에게는 각각 AI 답변 검토 요청 알림을 생성한다.
        verifyTargetMemberNotification(21L, NotificationType.QA_AI_REVIEW_REQUIRED,
                "대상 팀 검토 요청 알림");
        verifyTargetMemberNotification(22L, NotificationType.QA_AI_REVIEW_REQUIRED,
                "대상 팀 검토 요청 알림");
    }

    @Test
    void hidesAiFailureFromQuestionerAndRequestsManualAnswerFromTargetTeam() {
        // Given: AI 재시도가 모두 실패했고 대상 팀에는 두 명의 팀원이 있다.
        stubTargetTeam();
        stubTargetTeamMembers();
        when(messageFactory.createQaMessage(
                NotificationType.QA_ANSWER_WAITING,
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("담당 팀 답변 대기 알림");
        when(messageFactory.createQaMessage(
                NotificationType.QA_AI_FAILED,
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("직접 답변 요청 알림");

        QaAiFinalFailureEvent event = new QaAiFinalFailureEvent(
                100L,
                10L,
                2L,
                200L,
                "시스템 안내문"
        );

        // When: AI 최종 실패 이벤트를 처리한다.
        handler.handleAiFinalFailure(event);

        // Then: 질문자에게는 실패 알림이 아닌 일반적인 답변 대기 알림만 생성한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.QA_ANSWER_WAITING,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "담당 팀 답변 대기 알림"
        );

        // 실제 AI 실패 및 직접 답변 필요 알림은 대상 팀원에게만 생성한다.
        verifyTargetMemberNotification(21L, NotificationType.QA_AI_FAILED,
                "직접 답변 요청 알림");
        verifyTargetMemberNotification(22L, NotificationType.QA_AI_FAILED,
                "직접 답변 요청 알림");
    }

    @Test
    void notifiesQuestionerWhenTeamProvidesManualAnswer() {
        // Given: 대상 팀원이 AI 대신 직접 답변을 등록했다.
        stubTargetTeam();
        when(messageFactory.createQaMessage(
                NotificationType.QA_ANSWERED,
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("직접 답변 등록 알림");

        QaAnswerPublishedEvent event = new QaAnswerPublishedEvent(
                100L,
                200L,
                10L,
                2L,
                21L,
                QaAnswerPublishType.TEAM_PROVIDED
        );

        // When: 팀원 직접 답변 이벤트를 처리한다.
        handler.handleAnswerPublished(event);

        // Then: 질문자에게 새 답변이 등록됐다는 알림을 생성한다.
        verify(notificationCreateService).createNotification(
                10L,
                NotificationType.QA_ANSWERED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "직접 답변 등록 알림"
        );
        verifyNoInteractions(teamMemberRepository);
    }

    @Test
    void updatesUnreadRevisionNotificationInsteadOfCreatingDuplicate() {
        // Given: 담당 팀원이 기존 답변을 수정했다.
        stubTargetTeam();
        when(messageFactory.createQaMessage(
                NotificationType.QA_ANSWER_REVISED,
                "노디프로젝트",
                "마케팅팀"
        )).thenReturn("답변 수정 알림");

        QaAnswerPublishedEvent event = new QaAnswerPublishedEvent(
                100L,
                200L,
                10L,
                2L,
                21L,
                QaAnswerPublishType.ANSWER_REVISED
        );

        // When: 답변 수정 이벤트를 처리한다.
        handler.handleAnswerPublished(event);

        // Then: 질문자에게 동일 질문의 안 읽은 수정 알림을 생성하거나 갱신하도록 요청한다.
        verify(notificationCreateService).createOrUpdateUnread(
                10L,
                NotificationType.QA_ANSWER_REVISED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "답변 수정 알림"
        );
    }

    @Test
    void doesNotNotifyUserAboutOwnManualAnswerOrRevision() {
        // Given: 질문자 본인이 대상 팀원으로서 직접 답변한 특수 상황이다.
        stubTargetTeam();
        QaAnswerPublishedEvent event = new QaAnswerPublishedEvent(
                100L,
                200L,
                10L,
                2L,
                10L,
                QaAnswerPublishType.TEAM_PROVIDED
        );

        // When: 본인이 수행한 답변 이벤트를 처리한다.
        handler.handleAnswerPublished(event);

        // Then: 자신에게 자신의 행동을 알리는 알림은 만들지 않는다.
        verifyNoInteractions(notificationCreateService);
        verifyNoInteractions(messageFactory);
        verify(teamMemberRepository, never()).findAllByTeamWithUser(targetTeam);
    }

    private void stubTargetTeam() {
        when(teamRepository.findById(2L)).thenReturn(Optional.of(targetTeam));
        when(targetTeam.getProject()).thenReturn(project);
        when(project.getProjectId()).thenReturn(1L);
        when(project.getName()).thenReturn("노디프로젝트");
        when(targetTeam.getTeamId()).thenReturn(2L);
        when(targetTeam.getName()).thenReturn("마케팅팀");
    }

    private void stubTargetTeamMembers() {
        when(teamMemberRepository.findAllByTeamWithUser(targetTeam))
                .thenReturn(List.of(firstMember, secondMember));
        when(firstMember.getUser()).thenReturn(firstUser);
        when(secondMember.getUser()).thenReturn(secondUser);
        when(firstUser.getUserId()).thenReturn(21L);
        when(secondUser.getUserId()).thenReturn(22L);
    }

    private void verifyTargetMemberNotification(
            Long recipientId,
            NotificationType type,
            String message
    ) {
        verify(notificationCreateService).createNotification(
                recipientId,
                type,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                message
        );
    }
}
