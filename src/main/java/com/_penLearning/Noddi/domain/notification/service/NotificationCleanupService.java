package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    @Value("${scheduler.notification.retention-days:90}")
    private int retentionDays;

    @Value("${scheduler.notification.max-count-per-user:500}")
    private int maxCountPerUser;

    @Transactional
    public void cleanUpNotifications(){
        cleanUpExcessNotifications(maxCountPerUser);
        cleanUpOldNotifications(retentionDays);
    }

    @Transactional
    public int cleanUpOldNotifications(int days){
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        int deleted = notificationRepository.bulkDeleteOldNotifications(threshold);
        if (deleted > 0) {
            log.info("[NotificationCleanup] {}일 경과 알림 {}건을 삭제했습니다.", days, deleted);
        }
        return deleted;
    }

    public int cleanUpExcessNotifications(int maxCount) {
        List<Long> userIds = notificationRepository.findUserIdsWithExcessNotifications(maxCount);
        int totalDeleted = 0;

        for(Long userId : userIds){
            List<Notification> cutoffs = notificationRepository.findCutoffNotification(
                    userId, PageRequest.of(maxCount - 1, 1)
            );

            if(cutoffs.isEmpty()){
                Notification cutoff = cutoffs.getFirst();
                int deleted = notificationRepository.bulkDeleteExcessNotifications(
                        userId,
                        cutoff.getOccurredAt(),
                        cutoff.getNotificationId()
                );
                totalDeleted += deleted;
            }
        }
        if(totalDeleted > 0){
            log.info("[NotificationCleanup] 사용자별 최대 {}개 초과 알림 총 {}건을 삭제했습니다.", maxCount, totalDeleted);
        }
        return totalDeleted;
    }
}
