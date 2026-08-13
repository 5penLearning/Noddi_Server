package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
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

// 회의 전사를 실제로 Pinecone에 넣는 전체 과정을 제어함
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingKnowledgeIndexService {

    private final MeetingKnowledgeSourceReader sourceReader;
    private final QaKnowledgeIndexStateService indexStateService;
    private final KnowledgeDocumentFactory documentFactory;
    private final VectorStore vectorStore;

    public MeetingKnowledgeIndexResult index(Long meetingId) {
        KnowledgeSourceContent source = sourceReader.read(meetingId);

        // 회의 전사를 공통 소스 형식으로 전달하여 팀 작성 텍스트와 같은 색인 규칙을 사용한다.
        SourceSyncResult transcriptResult = synchronize(source);

        return new MeetingKnowledgeIndexResult(
                meetingId,
                transcriptResult.indexedChunkCount(),
                transcriptResult.skippedSourceCount()
        );
    }

    private SourceSyncResult synchronize(KnowledgeSourceContent source) {
        Optional<KnowledgeIndexState> previousState = indexStateService.get(
                source.sourceId(),
                source.sourceType()
        );

        if (source.content() == null || source.content().isBlank()) {
            removePreviousSource(source.sourceId(), source.sourceType(), previousState);
            return SourceSyncResult.skipped();
        }

        String contentHash = sha256(source.content());
        if (previousState.map(KnowledgeIndexState::contentHash).filter(contentHash::equals).isPresent()) {
            return SourceSyncResult.skipped();
        }

        List<Document> documents = documentFactory.create(source);

        // Pinecone 저장이 성공한 뒤에만 MySQL 색인 상태를 갱신한다.
        vectorStore.add(documents);

        previousState.ifPresent(state -> deleteStaleDocuments(
                source.sourceId(),
                source.sourceType(),
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
                "[MeetingKnowledgeIndex] 색인 완료: sourceId={}, sourceType={}, chunkCount={}",
                source.sourceId(),
                source.sourceType(),
                documents.size()
        );
        return SourceSyncResult.indexed(documents.size());
    }

    private void removePreviousSource(
            Long sourceId,
            SourceType sourceType,
            Optional<KnowledgeIndexState> previousState
    ) {
        previousState.ifPresent(state -> {
            List<String> ids = documentFactory.documentIds(
                    sourceId,
                    sourceType,
                    0,
                    state.chunkCount()
            );
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
            }
            indexStateService.remove(sourceId, sourceType);
        });
    }

    private void deleteStaleDocuments(
            Long sourceId,
            SourceType sourceType,
            int newChunkCount,
            int previousChunkCount
    ) {
        if (previousChunkCount <= newChunkCount) {
            return;
        }

        vectorStore.delete(documentFactory.documentIds(
                sourceId,
                sourceType,
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

    private record SourceSyncResult(int indexedChunkCount, int skippedSourceCount) {

        private static SourceSyncResult indexed(int chunkCount) {
            return new SourceSyncResult(chunkCount, 0);
        }

        private static SourceSyncResult skipped() {
            return new SourceSyncResult(0, 1);
        }
    }
}
