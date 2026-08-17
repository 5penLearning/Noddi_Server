package com._penLearning.Noddi.domain.qa.scheduler;

import com._penLearning.Noddi.domain.qa.event.QaQuestionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 정상적인 AI 호출 실패를 짧은 지수 backoff 후 다시 실행한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class QaAiRetryScheduler {

    private static final int MAX_BACKOFF_EXPONENT = 10;

    private final TaskScheduler taskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${qa.ai.retry-base-delay-millis:2000}")
    private long retryBaseDelayMillis;

    public void scheduleRetry(Long questionId, int failedAttempt) {
        int exponent = Math.min(Math.max(failedAttempt - 1, 0), MAX_BACKOFF_EXPONENT);
        long delayMillis = retryBaseDelayMillis * (1L << exponent);
        Instant retryAt = Instant.now().plusMillis(delayMillis);

        taskScheduler.schedule(
                () -> publishRetryEvent(questionId),
                retryAt
        );
    }

    private void publishRetryEvent(Long questionId) {
        try {
            eventPublisher.publishEvent(new QaQuestionCreatedEvent(questionId));
        } catch (RuntimeException exception) {
            // 예약 재시도 자체가 실패해도 장기 복구 스케줄러가 FAILED 질문을 다시 수습한다.
            log.error("Q&A AI retry event publishing failed. questionId={}", questionId, exception);
        }
    }
}
