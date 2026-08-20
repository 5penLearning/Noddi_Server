package com._penLearning.Noddi.domain.qa.rag.generation;

import com._penLearning.Noddi.domain.qa.rag.retrieval.KnowledgeRetriever;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 팀 지식 검색부터 OpenAI 답변 스트리밍까지 RAG 생성 흐름을 연결한다.
 * 반환된 Flux의 각 문자열은 완성된 답변이 아니라 모델이 순서대로 생성한 답변 조각이다.
 */
@Component
@ConditionalOnProperty(
        name = "spring.ai.model.chat",
        havingValue = "openai",
        matchIfMissing = true
)
public class QaRagAnswerGenerator {

    public static final String INSUFFICIENT_EVIDENCE_MESSAGE = "제공된 팀 자료에서 확인할 수 없습니다."; // 검색 결과가 없을 때 사용되는 고정 답변

    private final KnowledgeRetriever knowledgeRetriever; // Pinecone에서 해당 팀의 관련 자료 검색
    private final QaRagPromptFactory promptFactory; // 질문과 검색된 자료로 RAG 프롬프트 생성
    private final ChatModel chatModel; // 완성된 프롬프트를 OpenAI에 보내고 답변을 스트리밍으로 받음

    public QaRagAnswerGenerator(
            KnowledgeRetriever knowledgeRetriever,
            QaRagPromptFactory promptFactory,
            ChatModel chatModel
    ) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.promptFactory = promptFactory;
        this.chatModel = chatModel;
    }

    // 반환값인 Flux<String>는 완성된 답변 하나가 아니라 실시간으로 생성되는 답변 조각들이 들어옴
    public Flux<String> generate(Long teamId, String question) {
        return prepare(teamId, question)
                .flatMapMany(QaRagGeneration::answerChunks);
    }

    // Pinecone을 재검색하지 않고 답변과 출처를 함께 제공
    public Mono<QaRagGeneration> prepare(Long teamId, String question) {
        Assert.notNull(teamId, "팀 아이디는 필수 입력값입니다.");
        Assert.hasText(question, "질문은 필수 입력값입니다.");

        // Pinecone 검색은 블로킹 I/O이므로 스트리밍 처리 스레드와 분리한다.
        return Mono.fromCallable(() -> knowledgeRetriever.retrieve(teamId, question)) // 자료 검색 호출
                .subscribeOn(Schedulers.boundedElastic()) // 검색 응답을 기다리는 동안 스레드가 멈출 수 있으므로 분리
                .map(knowledgeList -> new QaRagGeneration(
                        knowledgeList,
                        generateFrom(question, knowledgeList)
                ));
    }

    // generate를 통해 받은 검색 결과로 AI 답변을 생성함
    private Flux<String> generateFrom(String question, List<RetrievedKnowledge> knowledgeList) {
        if (knowledgeList.isEmpty()) {
            // 관련 자료를 찾지 못하면 모델을 호출하지 않아 환각과 불필요한 API 비용을 방지한다.
            return Flux.just(INSUFFICIENT_EVIDENCE_MESSAGE);
        }

        // 프롬프트 생성
        Prompt prompt = promptFactory.create(question, knowledgeList);

        // 만들어진 프롬프트를 OpenAI에 전달
        return chatModel.stream(prompt)
                .handle((response, sink) -> {
                    String text = extractText(response);
                    if (StringUtils.hasText(text)) { // 유효한 텍스트만 전달
                        sink.next(text);
                    }
                });
    }

    // ChatResponse에서 실제 답변 문자열만 안전하게 꺼내는 메서드
    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }
}
