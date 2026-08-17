package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.code.NotificationErrorCode;
import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;

    /**
     * 사용자가 개별 알림의 자세히보기를 눌렀을 때 호출한다.
     *
     * 사용자 ID까지 함께 조회하므로 다른 사용자의 알림을 읽음 처리할 수 없다.
     */
    @Transactional
    public void markAsRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                getOwnedNotificationOrThrow(userId, notificationId);

        // 이미 읽은 알림이어도 같은 결과를 반환하는 멱등 처리
        notification.markAsRead();
    }

    /**
     * AI 검토 묶음의 자세히보기를 눌렀을 때
     * 현재 사용자에게 속한 묶음 전체를 읽음 처리한다.
     */
    @Transactional
    public void markGroupAsRead(
            Long userId,
            Long projectId,
            Long teamId,
            NotificationType type
    ) {
        validateGroupType(type);

        List<Notification> notifications =
                notificationRepository.findUnreadGroup(
                        userId,
                        projectId,
                        teamId,
                        type
                );

        notifications.forEach(Notification::markAsRead);
    }

    /**
     * 개별 알림의 X 버튼을 눌렀을 때 호출한다.
     *
     * hide()는 읽음과 숨김을 동시에 처리하므로
     * 숨긴 알림이 헤더의 안 읽은 개수에 남지 않는다.
     */
    @Transactional
    public void hide(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                getOwnedNotificationOrThrow(userId, notificationId);

        notification.hide();
    }

    /**
     * 묶음 알림의 X 버튼을 눌렀을 때
     * 화면에 표시된 해당 묶음만 숨긴다.
     *
     * read를 전달받는 이유는 ALL 조회에서 읽은 묶음과
     * 안 읽은 묶음이 별도로 표시되기 때문이다.
     */
    @Transactional
    public void hideGroup(
            Long userId,
            Long projectId,
            Long teamId,
            NotificationType type,
            boolean read
    ) {
        validateGroupType(type);

        List<Notification> notifications =
                notificationRepository.findVisibleGroupByReadStatus(
                        userId,
                        projectId,
                        teamId,
                        type,
                        read
                );

        notifications.forEach(Notification::hide);
    }

    /**
     * 질문 상태가 바뀌어 더 이상 행동할 필요가 없는 알림을 숨긴다.
     *
     * 예:
     * - 직접 답변 완료 후 QA_ANSWER_WAITING, QA_AI_FAILED 제거
     * - AI 답변 수정 후 QA_AI_REVIEW_REQUIRED 제거
     *
     * 이 메서드는 컨트롤러에서 직접 노출하지 않고
     * Q&A 이벤트 핸들러 내부에서만 사용한다.
     */
    @Transactional
    public void resolveQuestionNotifications(
            Long questionId,
            Set<NotificationType> types
    ) {
        if (types == null || types.isEmpty()) {
            return;
        }

        List<Notification> notifications =
                notificationRepository
                        .findVisibleNotificationsByReferenceAndTypes(
                                NotificationReferenceType.QA_QUESTION,
                                questionId,
                                types
                        );

        /*
         * 단순 읽음이 아니라 숨김 처리한다.
         *
         * 답변이 이미 등록됐는데도
         * “직접 답변해 주세요” 같은 만료된 행동 알림이
         * ALL 목록에 남으면 사용자에게 잘못된 상태를 보여주기 때문이다.
         */
        notifications.forEach(Notification::hide);
    }

    private Notification getOwnedNotificationOrThrow(
            Long userId,
            Long notificationId
    ) {
        return notificationRepository.findOwnedNotification(
                        notificationId,
                        userId
                )
                .orElseThrow(() ->
                        new GeneralException(
                                NotificationErrorCode.NOTIFICATION_NOT_FOUND
                        )
                );
    }

    private void validateGroupType(NotificationType type) {
        if (type != NotificationType.QA_AI_REVIEW_REQUIRED) {
            throw new GeneralException(
                    NotificationErrorCode.INVALID_NOTIFICATION_GROUP
            );
        }
    }
}