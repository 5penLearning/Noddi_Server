package com._penLearning.Noddi.domain.meeting.scheduler;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
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
public class MeetingAiRecoveryScheduler {

    private final MeetingRepository meetingRepository;

    @Value("${scheduler.meeting-ai.processing-timeout-minutes:30}")
    private long processingTimeoutMinutes;

    @Scheduled(
            cron = "${scheduler.meeting-ai.recovery-cron:0 */5 * * * *}",
            zone = "${scheduler.meeting-ai.zone:Asia/Seoul}"
    )
    @Transactional
    public void failStuckProcessingMeetings() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(processingTimeoutMinutes);

        int recoveredCount = meetingRepository.failStuckAiProcessing(
                threshold,
                AiStatus.PROCESSING,
                AiStatus.FAILED
        );

        if (recoveredCount > 0) {
            log.warn(
                    "[MeetingAiRecoveryScheduler] 제한 시간({}분)을 초과한 AI 작업 {}건을 FAILED로 변경했습니다.",
                    processingTimeoutMinutes,
                    recoveredCount
            );
        }
    }
}
