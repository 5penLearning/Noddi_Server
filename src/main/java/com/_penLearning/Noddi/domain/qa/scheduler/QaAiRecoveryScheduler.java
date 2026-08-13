package com._penLearning.Noddi.domain.qa.scheduler;

import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.event.QaQuestionCreatedEvent;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.qa.service.QaAiAnswerLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 서버 종료나 외부 API 오류로 멈춘 AI 답변 작업을 제한된 횟수 안에서 다시 시작한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class QaAiRecoveryScheduler {

    private static final List<QaStatus> RECOVERABLE_STATUSES = List.of(
            QaStatus.PENDING,
            QaStatus.PROCESSING,
            QaStatus.FAILED
    );

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAiAnswerLifecycleService answerLifecycleService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${scheduler.qa-ai.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    @Scheduled(
            cron = "${scheduler.qa-ai.recovery-cron:0 */5 * * * *}",
            zone = "${scheduler.qa-ai.zone:Asia/Seoul}"
    )
    @Transactional
    public void retryStalledAnswers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<QaQuestion> candidates = qaQuestionRepository
                .findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(RECOVERABLE_STATUSES, threshold);

        int recoveredCount = 0;
        for (QaQuestion question : candidates) {
            if (answerLifecycleService.prepareRecovery(question.getQuestionId(), threshold)) {
                eventPublisher.publishEvent(new QaQuestionCreatedEvent(question.getQuestionId()));
                recoveredCount++;
            }
        }

        if (recoveredCount > 0) {
            log.warn(
                    "[QaAiRecoveryScheduler] 멈춘 AI 답변 작업을 재시도합니다. count={}, timeoutMinutes={}",
                    recoveredCount,
                    processingTimeoutMinutes
            );
        }
    }
}
