package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationCreateService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(
            Long recipientId,
            NotificationType type,
            NotificationReferenceType referenceType,
            Long referenceId,
            Long projectId,
            Long teamId,
            String message
    ){
        User recipient = getUserOrThrow(recipientId);

        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .projectId(projectId)
                .teamId(teamId)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void createOrUpdateUnread(
            Long recipientId,
            NotificationType type,
            NotificationReferenceType referenceType,
            Long referenceId,
            Long projectId,
            Long teamId,
            String message
    ){
        Optional<Notification> existingNotification = notificationRepository.findLatestUnreadNotification(
                recipientId,
                type,
                referenceType,
                referenceId
        );

        // 동일한 안 읽은 알림이 있으면 새로 만들지 않고 갱신한다.
        if(existingNotification.isPresent()){
            existingNotification.get().updateMessage(message);
            return;
        }
        // 기존 알림이 없으면 새로운 알림을 저장한다.
        createNotification(
                recipientId,
                type,
                referenceType,
                referenceId,
                projectId,
                teamId,
                message);


    }
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new GeneralException(UserErrorCode.USER_NOT_FOUND)
                );
    }
}
