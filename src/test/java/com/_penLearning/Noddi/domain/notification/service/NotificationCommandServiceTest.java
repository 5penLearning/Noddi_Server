package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private User recipient;

    @Test
    void marksOwnedNotificationAsRead() {
        // Given: 현재 사용자가 소유한 안 읽은 알림이 있다.
        Notification notification = notification(NotificationType.QA_ANSWERED);
        when(notificationRepository.findOwnedNotification(100L, 10L))
                .thenReturn(Optional.of(notification));

        // When: 사용자가 알림의 자세히보기를 누른다.
        service().markAsRead(10L, 100L);

        // Then: 해당 알림만 읽음 상태로 변경한다.
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.isHidden()).isFalse();
    }

    @Test
    void rejectsReadRequestForMissingOrOtherUsersNotification() {
        // Given: 알림 ID와 사용자 ID를 동시에 만족하는 알림이 없다.
        when(notificationRepository.findOwnedNotification(100L, 10L))
                .thenReturn(Optional.empty());

        // When & Then: 다른 사용자의 알림을 추측해도 읽음 처리할 수 없다.
        assertThatThrownBy(() -> service().markAsRead(10L, 100L))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void marksAllUnreadNotificationsInReviewGroupAsRead() {
        // Given: 같은 사용자·프로젝트·팀에 안 읽은 AI 검토 알림이 두 개 있다.
        Notification first = notification(NotificationType.QA_AI_REVIEW_REQUIRED);
        Notification second = notification(NotificationType.QA_AI_REVIEW_REQUIRED);
        when(notificationRepository.findUnreadGroup(
                10L,
                1L,
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED
        )).thenReturn(List.of(first, second));

        // When: 사용자가 묶음 자세히보기를 누른다.
        service().markGroupAsRead(
                10L,
                1L,
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED
        );

        // Then: 묶음에 포함된 실제 알림을 모두 읽음 처리한다.
        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
    }

    @Test
    void hidesOwnedNotificationAndRemovesItFromUnreadCount() {
        // Given: 현재 사용자가 소유한 안 읽은 알림이 있다.
        Notification notification = notification(NotificationType.TEAM_INVITE);
        when(notificationRepository.findOwnedNotification(100L, 10L))
                .thenReturn(Optional.of(notification));

        // When: 사용자가 X 버튼을 누른다.
        service().hide(10L, 100L);

        // Then: 숨김과 읽음을 동시에 적용한다.
        assertThat(notification.isHidden()).isTrue();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void hidesOnlyGroupMatchingDisplayedReadStatus() {
        // Given: ALL 목록에 표시된 안 읽은 AI 검토 묶음이 있다.
        Notification first = notification(NotificationType.QA_AI_REVIEW_REQUIRED);
        Notification second = notification(NotificationType.QA_AI_REVIEW_REQUIRED);
        when(notificationRepository.findVisibleGroupByReadStatus(
                10L,
                1L,
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                false
        )).thenReturn(List.of(first, second));

        // When: 사용자가 해당 묶음의 X 버튼을 누른다.
        service().hideGroup(
                10L,
                1L,
                2L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                false
        );

        // Then: 화면에 표시된 묶음의 실제 알림을 모두 읽음·숨김 처리한다.
        assertThat(first.isHidden()).isTrue();
        assertThat(first.isRead()).isTrue();
        assertThat(second.isHidden()).isTrue();
        assertThat(second.isRead()).isTrue();
    }

    @Test
    void rejectsUnsupportedGroupTypeBeforeRepositoryLookup() {
        // 개별 답변 알림은 묶음 처리 대상이 아니므로 요청 자체를 거부한다.
        assertThatThrownBy(() -> service().markGroupAsRead(
                10L,
                1L,
                2L,
                NotificationType.QA_ANSWERED
        )).isInstanceOf(GeneralException.class);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void hidesResolvedQuestionNotificationsAcrossRecipients() {
        // Given: 직접 답변 완료로 더 이상 유효하지 않은 대기·실패 알림이 있다.
        Notification waiting = notification(NotificationType.QA_ANSWER_WAITING);
        Notification failed = notification(NotificationType.QA_AI_FAILED);
        Set<NotificationType> resolvedTypes = Set.of(
                NotificationType.QA_ANSWER_WAITING,
                NotificationType.QA_AI_FAILED
        );
        when(notificationRepository.findVisibleNotificationsByReferenceAndTypes(
                NotificationReferenceType.QA_QUESTION,
                200L,
                resolvedTypes
        )).thenReturn(List.of(waiting, failed));

        // When: Q&A 이벤트 핸들러가 질문의 만료된 알림을 정리한다.
        service().resolveQuestionNotifications(200L, resolvedTypes);

        // Then: 읽음 여부와 관계없이 더 이상 유효하지 않은 알림을 숨긴다.
        assertThat(waiting.isHidden()).isTrue();
        assertThat(waiting.isRead()).isTrue();
        assertThat(failed.isHidden()).isTrue();
        assertThat(failed.isRead()).isTrue();
    }

    private NotificationCommandService service() {
        return new NotificationCommandService(notificationRepository);
    }

    private Notification notification(NotificationType type) {
        return Notification.builder()
                .user(recipient)
                .type(type)
                .referenceType(NotificationReferenceType.QA_QUESTION)
                .referenceId(200L)
                .projectId(1L)
                .teamId(2L)
                .message("알림 문구")
                .build();
    }
}
