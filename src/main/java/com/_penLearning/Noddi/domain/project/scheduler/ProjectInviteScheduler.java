package com._penLearning.Noddi.domain.project.scheduler;

import com._penLearning.Noddi.domain.project.entity.InviteStatus;
import com._penLearning.Noddi.domain.project.repository.ProjectInviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectInviteScheduler {
    private final ProjectInviteRepository projectInviteRepository;

    @Value("${scheduler.invite.valid-days:7}")
    private int validDays;

    // 기본값은 매시간 정각 실행
    @Scheduled(
            cron = "${scheduler.invite.cron:0 0 * * * *}",
            zone = "${scheduler.invite.zone:Asia/Seoul}"
    )
    @Transactional
    public void expireOldInvitations() {
        // 설정된 유효기간을 기준으로 만료 기준 시각 계산
        LocalDateTime threshold = LocalDateTime.now().minusDays(validDays);

        // 기준선 이전에 생성된 PENDING 상태 초대장을 EXPIRED로 일괄 변경
        int expiredCount = projectInviteRepository.bulkExpireInvitations(
                threshold,
                InviteStatus.PENDING,
                InviteStatus.EXPIRED
        );

        if (expiredCount > 0) {
            log.info("[ProjectInviteScheduler] 유효기간({}일)이 지난 초대장 {}건을 EXPIRED 상태로 일괄 변경했습니다.", validDays, expiredCount);
        }
    }
}
