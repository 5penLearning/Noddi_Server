package com._penLearning.Noddi.domain.qa.event;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagGeneration;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.qa.service.QaAiAnswerLifecycleService;
import com._penLearning.Noddi.domain.qa.service.QaAnswerStreamService;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 질문 저장이 완료된 뒤 RAG 답변 생성을 시작한다.
 *
 * 생성되는 답변 조각은 SSE 구독자에게 실시간으로 전달하고,
 * 생성이 끝나면 최종 답변과 근거를 DB에 저장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "spring.ai.model.chat",
        havingValue = "openai",
        matchIfMissing = true
)
public class QaQuestionCreatedEventHandler {

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAiAnswerLifecycleService answerLifecycleService;
    private final QaRagAnswerGenerator answerGenerator;
    private final QaAnswerStreamService answerStreamService;

    @Async // 질문 등록 API가 OpenAI 응답을 기다리지 않음
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 질문 DB 저장이 확정된 후에만 AI 생성을 시작함
    public void handle(QaQuestionCreatedEvent event) {
        Long questionId = event.questionId();

        // 같은 이벤트가 중복 전달되더라도 하나의 생성 작업만 PROCESSING 상태를 획득한다.
        if (!answerLifecycleService.tryStart(questionId)) { // 중복 이벤트가 들어와도 OpenAI를 여러 번 호출하지 않도록 방지
            return;
        }

        /*
         * 이전 실패나 재시도 과정에서 남은 누적 답변을 초기화한다.
         *
         * SSE 초기화가 실패하더라도 AI 답변 생성과 DB 저장은 계속되어야 하므로
         * 안전하게 감싼 메서드를 사용한다.
         */
        startStreamSafely(questionId);

        try {
            QaQuestion question = qaQuestionRepository.findByIdWithTeam(questionId)
                    .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));

            QaRagGeneration generation = answerGenerator.prepare(
                            question.getTargetTeam().getTeamId(),
                            question.getContent()
                    )
                    .block();

            if (generation == null) {
                throw new GeneralException(QaErrorCode.AI_ANSWER_GENERATION_FAILED);
            }

            String answer = generation.answerChunks()
                    .doOnNext(chunk -> publishChunkSafely(questionId, chunk))
                    .collectList()
                    .map(chunks -> String.join("", chunks))
                    .filter(content -> !content.isBlank())
                    .switchIfEmpty(reactor.core.publisher.Mono.error(
                            new GeneralException(QaErrorCode.AI_ANSWER_GENERATION_FAILED)
                    ))
                    .block();

            Long answerId = answerLifecycleService.complete(questionId, answer, generation.sources()); // 답변과 근거 저장

            publishCompletedSafely(questionId, answerId, answer);
        } catch (Exception exception) {
            failQuestionSafely(questionId);
            publishFailedSafely(questionId);
            log.error("Q&A AI answer generation failed. questionId={}", questionId, exception);
        }
    }

    /**
     * SSE 처리 장애가 AI 답변 생성 자체를 중단시키지 않도록
     * 스트림 시작 과정의 예외를 이 메서드 안에서 처리한다.
     */
    private void startStreamSafely(Long questionId) {
        try {
            answerStreamService.start(questionId);
        } catch (RuntimeException exception) {
            log.warn(
                    "Q&A SSE stream initialization failed. questionId={}",
                    questionId,
                    exception
            );
        }
    }

    /**
     * AI가 생성한 조각을 SSE로 전달한다.
     *
     * 사용자가 브라우저를 닫거나 네트워크가 끊어져 SSE 전송이 실패하더라도
     * AI 최종 답변은 정상적으로 DB에 저장되어야 한다.
     */
    private void publishChunkSafely(
            Long questionId,
            String chunk
    ) {
        try {
            answerStreamService.publishChunk(questionId, chunk);
        } catch (RuntimeException exception) {
            log.warn(
                    "Q&A SSE chunk publishing failed. questionId={}",
                    questionId,
                    exception
            );
        }
    }

    /**
     * 최종 답변의 DB 저장이 끝났음을 SSE 구독자에게 알린다.
     * DB에는 이미 답변이 저장됐는데 실패 처리까지 시도하는 이상한 흐름이 생길 수 있어 책임을 분리합니다.
     */
    private void publishCompletedSafely(
            Long questionId,
            Long answerId,
            String content
    ) {
        try {
            answerStreamService.publishCompleted(
                    questionId,
                    answerId,
                    content
            );
        } catch (RuntimeException exception) {
            /*
             * COMPLETED 전송이 실패해도 DB에는 이미 답변이 저장되어 있다.
             * 따라서 질문을 FAILED로 변경하면 안 된다.
             */
            log.warn(
                    "Q&A SSE completion publishing failed. questionId={}, answerId={}",
                    questionId,
                    answerId,
                    exception
            );
        }
    }

    /**
     * AI 답변 생성 실패를 SSE 구독자에게 알린다.
     */
    private void publishFailedSafely(Long questionId) {
        try {
            answerStreamService.publishFailed(questionId);
        } catch (RuntimeException exception) {
            log.warn(
                    "Q&A SSE failure publishing failed. questionId={}",
                    questionId,
                    exception
            );
        }
    }

    /**
     * 질문 상태를 FAILED로 변경한다.
     *
     * 실패 상태 저장 자체에서 추가 예외가 발생하더라도 원래 발생한
     * AI 생성 예외가 사라지지 않게 별도로 처리한다.
     */
    private void failQuestionSafely(Long questionId) {
        try {
            answerLifecycleService.fail(questionId);
        } catch (RuntimeException exception) {
            log.error(
                    "Q&A question failure state update failed. questionId={}",
                    questionId,
                    exception
            );
        }
    }
}
