package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 긴 전사 내용을 Pinecone에 넣기 적합한 작은 조각으로 나눈다
@Component
public class KnowledgeDocumentFactory {

    // 각 청크에 들어가는 메타데이터들
    public static final String TEAM_ID = "team_id"; // 다름 팀 자료가 섞이지 않도록 함
    public static final String SOURCE_ID = "source_id";
    public static final String SOURCE_TYPE = "source_type";
    public static final String SOURCE_TITLE = "source_title";
    public static final String CHUNK_INDEX = "chunk_index";

    private static final int CHUNK_SIZE = 500;
    private static final int MIN_CHUNK_SIZE_CHARS = 50;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 10;
    private static final int MAX_NUM_CHUNKS = 1_000;

    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder()
            .withChunkSize(CHUNK_SIZE)
            .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
            .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
            .withMaxNumChunks(MAX_NUM_CHUNKS)
            .withKeepSeparator(true)
            .build();

    public List<Document> create(KnowledgeSourceContent source) {
        List<Document> splitDocuments = textSplitter.apply(List.of(
                Document.builder().text(source.content()).build()
        ));

        List<Document> documents = new ArrayList<>(splitDocuments.size());
        for (int index = 0; index < splitDocuments.size(); index++) {
            documents.add(Document.builder()
                    .id(documentId(source.sourceId(), source.sourceType(), index))
                    .text(splitDocuments.get(index).getText())
                    .metadata(Map.of(
                            TEAM_ID, source.teamId().toString(),
                            SOURCE_ID, source.sourceId().toString(),
                            SOURCE_TYPE, source.sourceType().name(),
                            SOURCE_TITLE, source.sourceTitle(),
                            CHUNK_INDEX, index
                    ))
                    .build());
        }
        return documents;
    }

    public List<String> documentIds(
            Long sourceId,
            SourceType sourceType,
            int fromInclusive,
            int toExclusive
    ) {
        List<String> ids = new ArrayList<>();
        for (int index = fromInclusive; index < toExclusive; index++) {
            ids.add(documentId(sourceId, sourceType, index));
        }
        return ids;
    }

    private String documentId(Long sourceId, SourceType sourceType, int chunkIndex) {
        return "knowledge-%s-%d-%d".formatted(
                sourceType.name().toLowerCase(),
                sourceId,
                chunkIndex
        );
    }
}
