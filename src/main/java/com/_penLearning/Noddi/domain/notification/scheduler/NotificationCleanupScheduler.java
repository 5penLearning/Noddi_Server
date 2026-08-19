package com._penLearning.Noddi.domain.notification.scheduler;

import com._penLearning.Noddi.domain.notification.service.NotificationCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationCleanupService notificationCleanupService;

    @Scheduled(
            cron = "${scheduler.notification.cron:0 0 3 * * *}",
            zone = "${scheduler.notification.zone:Asia/Seoul}"
    )
    public void cleanup() {
        log.info("[NotificationCleanupScheduler] 알림 보관 정책 정리 작업을 시작합니다.");
        notificationCleanupService.cleanUpNotifications();
        log.info("[NotificationCleanupScheduler] 알림 보관 정책 정리 작업이 완료되었습니다.");
    }
}
