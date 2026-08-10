package com._penLearning.Noddi.domain.team.scheduler;

import com._penLearning.Noddi.domain.team.entity.InviteStatus;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamInviteScheduler {

    private final TeamInviteRepository teamInviteRepository;

    // 매일 자정(00:00:00)에 실행되도록 Cron 표현식 설정
    @Scheduled(
            cron = "${scheduler.team-invite.cron:0 0 * * * *}",
            zone = "${scheduler.team-invite.zone:Asia/Seoul}"
    )    @Transactional
    public void expireOldInvitations() {
        // 기준선: 시스템 현재 시간으로부터 정확히 7일 전
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        // 기준선 이전에 생성된 PENDING 상태 초대장을 EXPIRED로 일괄 변경
        int expiredCount = teamInviteRepository.bulkExpireInvitations(
                threshold,
                InviteStatus.PENDING,
                InviteStatus.EXPIRED
        );

        if (expiredCount > 0) {
            log.info("[TeamInviteScheduler] 유효기간(7일)이 지난 초대장 {}건을 EXPIRED 상태로 일괄 변경했습니다.", expiredCount);
        }
    }
}
