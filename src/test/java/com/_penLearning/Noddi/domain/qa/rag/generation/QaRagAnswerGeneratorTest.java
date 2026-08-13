package com._penLearning.Noddi.domain.qa.rag.generation;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.retrieval.KnowledgeRetriever;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaRagAnswerGeneratorTest {

    @Mock
    private KnowledgeRetriever knowledgeRetriever;

    @Mock
    private QaRagPromptFactory promptFactory;

    @Mock
    private ChatModel chatModel;

    @Test
    void streamsAnswerChunksCreatedFromRetrievedKnowledge() {
        QaRagAnswerGenerator generator = createGenerator();
        List<RetrievedKnowledge> knowledgeList = List.of(new RetrievedKnowledge(
                "knowledge-transcript-20-0",
                20L,
                SourceType.TRANSCRIPT,
                "백엔드 배포 회의",
                "백엔드 서버의 최종 배포일은 8월 20일입니다.",
                0,
                0.529
        ));
        Prompt prompt = new Prompt("RAG 테스트 프롬프트");

        when(knowledgeRetriever.retrieve(10L, "백엔드는 언제 배포하나요?"))
                .thenReturn(knowledgeList);
        when(promptFactory.create("백엔드는 언제 배포하나요?", knowledgeList))
                .thenReturn(prompt);
        when(chatModel.stream(prompt)).thenReturn(Flux.just(
                response("백엔드 서버는 "),
                response("8월 20일에 배포합니다. [근거 1]")
        ));

        QaRagGeneration generation = generator.prepare(10L, "백엔드는 언제 배포하나요?")
                .block();
        List<String> chunks = generation.answerChunks()
                .collectList()
                .block();

        assertThat(generation.sources()).containsExactlyElementsOf(knowledgeList);
        assertThat(chunks).containsExactly(
                "백엔드 서버는 ",
                "8월 20일에 배포합니다. [근거 1]"
        );
        verify(knowledgeRetriever).retrieve(10L, "백엔드는 언제 배포하나요?");
        verify(promptFactory).create("백엔드는 언제 배포하나요?", knowledgeList);
        verify(chatModel).stream(prompt);
    }

    @Test
    void returnsFixedMessageWithoutCallingModelWhenKnowledgeIsEmpty() {
        QaRagAnswerGenerator generator = createGenerator();
        when(knowledgeRetriever.retrieve(10L, "근거가 없는 질문"))
                .thenReturn(List.of());

        List<String> chunks = generator.generate(10L, "근거가 없는 질문")
                .collectList()
                .block();

        assertThat(chunks).containsExactly(QaRagAnswerGenerator.INSUFFICIENT_EVIDENCE_MESSAGE);
        verify(promptFactory, never()).create(any(), any());
        verify(chatModel, never()).stream(any(Prompt.class));
    }

    @Test
    void ignoresEmptyChunksFromModelStream() {
        QaRagAnswerGenerator generator = createGenerator();
        List<RetrievedKnowledge> knowledgeList = List.of(new RetrievedKnowledge(
                "knowledge-transcript-20-0",
                20L,
                SourceType.TRANSCRIPT,
                "회의",
                "근거",
                0,
                0.5
        ));
        Prompt prompt = new Prompt("프롬프트");

        when(knowledgeRetriever.retrieve(10L, "질문")).thenReturn(knowledgeList);
        when(promptFactory.create("질문", knowledgeList)).thenReturn(prompt);
        when(chatModel.stream(prompt)).thenReturn(Flux.just(
                response(""),
                response("유효한 답변")
        ));

        List<String> chunks = generator.generate(10L, "질문")
                .collectList()
                .block();

        assertThat(chunks).containsExactly("유효한 답변");
    }

    private QaRagAnswerGenerator createGenerator() {
        return new QaRagAnswerGenerator(knowledgeRetriever, promptFactory, chatModel);
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
