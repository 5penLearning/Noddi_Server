package com._penLearning.Noddi.domain.qa.rag.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 회의 전사와 팀 공유페이지처럼 종류가 다른 텍스트 원본을 동일한 규칙으로 Pinecone과 동기화한다.
 * 원본이 바뀌지 않았다면 임베딩 API 호출을 생략하고, 청크 수가 줄면 남은 이전 벡터도 삭제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexService {

    private final QaKnowledgeIndexStateService indexStateService;
    private final KnowledgeDocumentFactory documentFactory;
    private final VectorStore vectorStore;

    public KnowledgeIndexResult synchronize(KnowledgeSourceContent source) {
        Optional<KnowledgeIndexState> previousState = indexStateService.get(
                source.sourceId(),
                source.sourceType()
        );

        if (source.content() == null || source.content().isBlank()) {
            removePreviousSource(source, previousState);
            return KnowledgeIndexResult.skippedResult();
        }

        String contentHash = sha256(source.content());
        if (previousState.map(KnowledgeIndexState::contentHash).filter(contentHash::equals).isPresent()) {
            return KnowledgeIndexResult.skippedResult();
        }

        List<Document> documents = documentFactory.create(source);
        vectorStore.add(documents);

        previousState.ifPresent(state -> deleteStaleDocuments(
                source,
                documents.size(),
                state.chunkCount()
        ));

        indexStateService.recordSuccess(
                source.sourceId(),
                source.teamId(),
                source.sourceType(),
                contentHash,
                documents.size()
        );

        log.info(
                "[KnowledgeIndex] 인덱싱 완료: sourceId={}, sourceType={}, chunkCount={}",
                source.sourceId(),
                source.sourceType(),
                documents.size()
        );
        return KnowledgeIndexResult.indexed(documents.size());
    }

    private void removePreviousSource(
            KnowledgeSourceContent source,
            Optional<KnowledgeIndexState> previousState
    ) {
        previousState.ifPresent(state -> {
            List<String> ids = documentFactory.documentIds(
                    source.sourceId(),
                    source.sourceType(),
                    0,
                    state.chunkCount()
            );
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
            }
            indexStateService.remove(source.sourceId(), source.sourceType());
        });
    }

    private void deleteStaleDocuments(
            KnowledgeSourceContent source,
            int newChunkCount,
            int previousChunkCount
    ) {
        if (previousChunkCount <= newChunkCount) {
            return;
        }

        vectorStore.delete(documentFactory.documentIds(
                source.sourceId(),
                source.sourceType(),
                newChunkCount,
                previousChunkCount
        ));
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
