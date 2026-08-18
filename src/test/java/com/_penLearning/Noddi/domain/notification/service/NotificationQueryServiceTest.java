package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.dto.NotificationFilter;
import com._penLearning.Noddi.domain.notification.dto.NotificationNavigationType;
import com._penLearning.Noddi.domain.notification.dto.NotificationResponseDto;
import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private NotificationMessageFactory messageFactory;
    @Mock private User recipient;
    @Mock private Team team;
    @Mock private Project project;

    @Test
    void groupsAiReviewNotificationsByTeamAndReadStatus() {
        // Given: 같은 팀에 안 읽은 AI 검토 알림 2개와 읽은 알림 1개가 있다.
        Notification unreadOld = notification(
                1L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                101L,
                false,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
        Notification unreadLatest = notification(
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                102L,
                false,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        );
        Notification read = notification(
                3L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                103L,
                true,
                LocalDateTime.of(2026, 8, 17, 9, 0)
        );

        stubVisibleNotifications(List.of(unreadLatest, unreadOld, read));
        when(notificationRepository.countUnreadNotifications(10L)).thenReturn(2L);
        stubTeamForGroupMessage();
        when(messageFactory.createQaReviewGroupMessage(
                "노디프로젝트",
                "마케팅팀",
                2
        )).thenReturn("안 읽은 AI 답변 2개");
        when(messageFactory.createQaReviewGroupMessage(
                "노디프로젝트",
                "마케팅팀",
                1
        )).thenReturn("읽은 AI 답변 1개");

        // When: 전체 알림을 조회한다.
        NotificationResponseDto.NotificationList response = service()
                .getNotifications(10L, NotificationFilter.ALL, 0, 20);

        // Then: 읽음 상태가 다른 두 개의 묶음 항목으로 분리한다.
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getUnreadCount()).isEqualTo(2);

        NotificationResponseDto.NotificationItem unreadGroup = response.getItems().stream()
                .filter(item -> !item.isRead())
                .findFirst()
                .orElseThrow();

        assertThat(unreadGroup.isGrouped()).isTrue();
        assertThat(unreadGroup.getNotificationId()).isNull();
        assertThat(unreadGroup.getCount()).isEqualTo(2);
        assertThat(unreadGroup.getMessage()).isEqualTo("안 읽은 AI 답변 2개");
        assertThat(unreadGroup.getGroupKey())
                .isEqualTo("QA_AI_REVIEW_REQUIRED:1:2:false");
        assertThat(unreadGroup.getNavigation().getType())
                .isEqualTo(NotificationNavigationType.QA_TEAM_FEED);
        assertThat(unreadGroup.getNavigation().getReferenceId()).isNull();
    }

    @Test
    void unreadFilterExcludesReadNotificationsBeforeGrouping() {
        // Given: 같은 팀에 읽은 검토 알림과 안 읽은 검토 알림이 하나씩 있다.
        Notification unread = notification(
                1L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                101L,
                false,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        );
        Notification read = notification(
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                102L,
                true,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );

        stubVisibleNotifications(List.of(unread, read));
        when(notificationRepository.countUnreadNotifications(10L)).thenReturn(1L);
        stubTeamForGroupMessage();
        when(messageFactory.createQaReviewGroupMessage(
                "노디프로젝트",
                "마케팅팀",
                1
        )).thenReturn("안 읽은 AI 답변 1개");

        // When: 안 읽은 알림만 조회한다.
        NotificationResponseDto.NotificationList response = service()
                .getNotifications(10L, NotificationFilter.UNREAD, 0, 20);

        // Then: 읽은 알림은 그룹을 만들기 전에 제외한다.
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().isRead()).isFalse();
        assertThat(response.getItems().getFirst().getCount()).isEqualTo(1);
    }

    @Test
    void mapsBothInviteTypesToMyInvitations() {
        // Given: 팀 초대와 프로젝트 초대 알림이 각각 하나씩 있다.
        Notification teamInvite = notification(
                1L,
                NotificationType.TEAM_INVITE,
                NotificationReferenceType.TEAM_INVITE,
                301L,
                false,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        );
        Notification projectInvite = notification(
                2L,
                NotificationType.PROJECT_INVITE,
                NotificationReferenceType.PROJECT_INVITE,
                401L,
                false,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
        ReflectionTestUtils.setField(projectInvite, "teamId", null);

        stubVisibleNotifications(List.of(teamInvite, projectInvite));
        when(notificationRepository.countUnreadNotifications(10L)).thenReturn(2L);

        // When: 전체 알림을 조회한다.
        NotificationResponseDto.NotificationList response = service()
                .getNotifications(10L, NotificationFilter.ALL, 0, 20);

        // Then: 두 초대 모두 마이페이지의 받은 초대장 화면으로 이동한다.
        assertThat(response.getItems())
                .allSatisfy(item -> assertThat(item.getNavigation().getType())
                        .isEqualTo(NotificationNavigationType.MY_INVITATIONS));
        assertThat(response.getItems())
                .extracting(item -> item.getNavigation().getReferenceId())
                .containsExactly(301L, 401L);
    }

    @Test
    void paginatesAfterGroupingAndOrdersByLatestOccurrence() {
        // Given: AI 검토 알림 2개는 한 항목으로 묶이고, 개별 알림 2개가 함께 존재한다.
        Notification reviewOld = notification(
                1L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                101L,
                false,
                LocalDateTime.of(2026, 8, 18, 9, 0)
        );
        Notification reviewLatest = notification(
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                NotificationReferenceType.QA_QUESTION,
                102L,
                false,
                LocalDateTime.of(2026, 8, 18, 12, 0)
        );
        Notification answered = notification(
                3L,
                NotificationType.QA_ANSWERED,
                NotificationReferenceType.QA_QUESTION,
                103L,
                false,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        );
        Notification invite = notification(
                4L,
                NotificationType.TEAM_INVITE,
                NotificationReferenceType.TEAM_INVITE,
                301L,
                false,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );

        stubVisibleNotifications(List.of(reviewLatest, answered, invite, reviewOld));
        when(notificationRepository.countUnreadNotifications(10L)).thenReturn(4L);
        stubTeamForGroupMessage();
        when(messageFactory.createQaReviewGroupMessage(
                "노디프로젝트",
                "마케팅팀",
                2
        )).thenReturn("AI 답변 2개");

        // When: 화면 항목을 2개씩 나누어 첫 페이지를 조회한다.
        NotificationResponseDto.NotificationList response = service()
                .getNotifications(10L, NotificationFilter.ALL, 0, 2);

        // Then: DB 알림 4개가 화면 항목 3개가 된 뒤 최신순으로 페이지를 자른다.
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(3);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getItems().get(0).isGrouped()).isTrue();
        assertThat(response.getItems().get(1).getType())
                .isEqualTo(NotificationType.QA_ANSWERED);
    }

    @Test
    void rejectsInvalidPageRequestBeforeAccessingRepositories() {
        // page와 size가 허용 범위를 벗어나면 DB를 조회하기 전에 요청을 거부한다.
        NotificationQueryService service = service();

        assertThatThrownBy(() -> service.getNotifications(
                10L,
                NotificationFilter.ALL,
                -1,
                20
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.getNotifications(
                10L,
                NotificationFilter.ALL,
                0,
                51
        )).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(notificationRepository);
    }

    private NotificationQueryService service() {
        return new NotificationQueryService(
                notificationRepository,
                teamRepository,
                messageFactory
        );
    }

    private void stubVisibleNotifications(List<Notification> notifications) {
        when(notificationRepository.findVisibleNotifications(
                eq(10L),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(notifications));
    }

    private void stubTeamForGroupMessage() {
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(team.getProject()).thenReturn(project);
        when(project.getName()).thenReturn("노디프로젝트");
        when(team.getName()).thenReturn("마케팅팀");
    }

    private Notification notification(
            Long notificationId,
            NotificationType type,
            NotificationReferenceType referenceType,
            Long referenceId,
            boolean read,
            LocalDateTime occurredAt
    ) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .projectId(1L)
                .teamId(2L)
                .message("저장된 알림 문구")
                .build();

        ReflectionTestUtils.setField(
                notification,
                "notificationId",
                notificationId
        );
        ReflectionTestUtils.setField(
                notification,
                "occurredAt",
                occurredAt
        );

        if (read) {
            notification.markAsRead();
        }

        return notification;
    }
}
