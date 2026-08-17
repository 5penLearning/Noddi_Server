package com._penLearning.Noddi.domain.qa.event;

import com._penLearning.Noddi.domain.qa.service.QaAnswerStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 최종 실패 DB 반영이 커밋된 뒤 SSE와 후속 알림이 사용할 이벤트 지점이다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class QaAiFinalFailureEventHandler {

    private final QaAnswerStreamService answerStreamService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(QaAiFinalFailureEvent event) {
        try {
            answerStreamService.publishTeamAnswerPending(
                    event.questionId(),
                    event.answerId(),
                    event.noticeContent()
            );
        } catch (RuntimeException exception) {
            // 최종 실패 상태와 안내 답변은 이미 커밋됐으므로 SSE 장애를 다시 DB 실패로 전파하지 않는다.
            log.warn("Q&A manual-required SSE publishing failed. questionId={}", event.questionId(), exception);
        }
    }
}
