package com._penLearning.Noddi.domain.qa.rag.generation;

import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import io.pinecone.clients.Pinecone;
import reactor.core.publisher.Flux;

import java.util.List;

//한 번의 Pinecone 검색으로 준비된 답변 스트림과 답변 생성에 제공된 근거 목록이다.
public record QaRagGeneration(
        List<RetrievedKnowledge> sources,
        Flux<String> answerChunks
) {
    public QaRagGeneration {
        sources = List.copyOf(sources);
    }
}
