package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCreateServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User recipient;

    @Test
    void createsNewNotificationForRecipient() {
        // Given: 알림을 받을 사용자가 존재한다.
        NotificationCreateService service = service();
        when(userRepository.findById(10L)).thenReturn(Optional.of(recipient));

        // When: Q&A 답변 등록 알림 생성을 요청한다.
        service.createNotification(
                10L,
                NotificationType.QA_ANSWERED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "질문에 새로운 답변이 등록됐어요."
        );

        // Then: 수신자와 네비게이션 정보가 모두 포함된 안 읽은 알림을 저장한다.
        ArgumentCaptor<Notification> notificationCaptor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUser()).isSameAs(recipient);
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.QA_ANSWERED);
        assertThat(savedNotification.getReferenceType())
                .isEqualTo(NotificationReferenceType.QA_QUESTION);
        assertThat(savedNotification.getReferenceId()).isEqualTo(100L);
        assertThat(savedNotification.getProjectId()).isEqualTo(1L);
        assertThat(savedNotification.getTeamId()).isEqualTo(2L);
        assertThat(savedNotification.getMessage()).isEqualTo("질문에 새로운 답변이 등록됐어요.");
        assertThat(savedNotification.isRead()).isFalse();
        assertThat(savedNotification.isHidden()).isFalse();
        assertThat(savedNotification.getOccurredAt()).isNotNull();
    }

    @Test
    void updatesExistingUnreadNotificationInsteadOfCreatingDuplicate() {
        // Given: 같은 질문에 대한 안 읽은 답변 수정 알림이 이미 존재한다.
        NotificationCreateService service = service();
        Notification existingNotification = Notification.builder()
                .user(recipient)
                .type(NotificationType.QA_ANSWER_REVISED)
                .referenceType(NotificationReferenceType.QA_QUESTION)
                .referenceId(100L)
                .projectId(1L)
                .teamId(2L)
                .message("이전 수정 알림")
                .build();
        LocalDateTime previousOccurredAt = existingNotification.getOccurredAt();

        when(notificationRepository.findLatestUnreadNotification(
                10L,
                NotificationType.QA_ANSWER_REVISED,
                NotificationReferenceType.QA_QUESTION,
                100L
        )).thenReturn(Optional.of(existingNotification));

        // When: 동일 답변에 대한 새로운 수정 알림 생성을 요청한다.
        service.createOrUpdateUnread(
                10L,
                NotificationType.QA_ANSWER_REVISED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "답변이 다시 수정됐어요."
        );

        // Then: 새 레코드를 저장하지 않고 기존 알림의 내용과 발생 시각만 갱신한다.
        assertThat(existingNotification.getMessage()).isEqualTo("답변이 다시 수정됐어요.");
        assertThat(existingNotification.getOccurredAt()).isAfterOrEqualTo(previousOccurredAt);
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void createsNotificationWhenUnreadDuplicateDoesNotExist() {
        // Given: 같은 질문에 대한 안 읽은 수정 알림이 없고 수신자는 존재한다.
        NotificationCreateService service = service();
        when(notificationRepository.findLatestUnreadNotification(
                10L,
                NotificationType.QA_ANSWER_REVISED,
                NotificationReferenceType.QA_QUESTION,
                100L
        )).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.of(recipient));

        // When: 답변 수정 알림 생성을 요청한다.
        service.createOrUpdateUnread(
                10L,
                NotificationType.QA_ANSWER_REVISED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "답변이 수정됐어요."
        );

        // Then: 새로운 알림을 저장한다.
        verify(notificationRepository).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    void rejectsCreationWhenRecipientDoesNotExist() {
        // Given: 알림 수신자 ID에 해당하는 사용자가 존재하지 않는다.
        NotificationCreateService service = service();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then: 알림 생성을 거부하고 저장소에는 접근하지 않는다.
        assertThatThrownBy(() -> service.createNotification(
                999L,
                NotificationType.QA_ANSWERED,
                NotificationReferenceType.QA_QUESTION,
                100L,
                1L,
                2L,
                "답변이 등록됐어요."
        )).isInstanceOf(GeneralException.class);

        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private NotificationCreateService service() {
        return new NotificationCreateService(notificationRepository, userRepository);
    }
}
