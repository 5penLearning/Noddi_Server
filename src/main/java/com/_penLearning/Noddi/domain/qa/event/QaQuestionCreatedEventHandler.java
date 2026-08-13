package com._penLearning.Noddi.domain.qa.event;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagGeneration;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.qa.service.QaAiAnswerLifecycleService;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 질문 저장이 완료된 뒤 RAG 답변 생성을 시작하고 최종 답변을 DB에 저장한다.
 * SSE 전송은 담당하지 않으며, 생성기가 내보낸 문자열 조각을 저장 목적으로만 합친다.
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

    @Async // 질문 등록 API가 OpenAI 응답을 기다리지 않음
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 질문 DB 저장이 확정된 후에만 AI 생성을 시작함
    public void handle(QaQuestionCreatedEvent event) {
        Long questionId = event.questionId();

        // 같은 이벤트가 중복 전달되더라도 하나의 생성 작업만 PROCESSING 상태를 획득한다.
        if (!answerLifecycleService.tryStart(questionId)) { // 중복 이벤트가 들어와도 OpenAI를 여러 번 호출하지 않도록 방지
            return;
        }

        try {
            QaQuestion question = qaQuestionRepository.findByIdWithTeam(questionId)
                    .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));

            QaRagGeneration generation = answerGenerator.prepare(
                            question.getTargetTeam().getTeamId(),
                            question.getContent()
                    )
                    .block();

            if (generation == null) {
                throw new IllegalStateException("RAG generation preparation completed without a result");
            }

            String answer = generation.answerChunks()
                    .collectList()
                    .map(chunks -> String.join("", chunks))
                    .filter(content -> !content.isBlank())
                    .switchIfEmpty(reactor.core.publisher.Mono.error(
                            new IllegalStateException("AI answer stream completed without content")
                    ))
                    .block();

            answerLifecycleService.complete(questionId, answer, generation.sources()); // 답변과 근거 저장
        } catch (Exception exception) {
            answerLifecycleService.fail(questionId); // API 호출 실패 또는 빈 답변이면 FAILED로 전환
            log.error("Q&A AI answer generation failed. questionId={}", questionId, exception);
        }
    }
}
