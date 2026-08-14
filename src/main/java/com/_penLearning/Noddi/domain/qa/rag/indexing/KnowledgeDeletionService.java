package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.QaKnowledgeIndex;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.repository.QaKnowledgeIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 원본 데이터 수명에 맞춰 Pinecone 청크와 MySQL 색인 상태를 함께 제거한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDeletionService {

    private final QaKnowledgeIndexRepository knowledgeIndexRepository;
    private final KnowledgeDocumentFactory documentFactory;
    private final VectorStore vectorStore;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteMeetingKnowledge(Long meetingId) {
        knowledgeIndexRepository.findBySourceIdAndSourceType(meetingId, SourceType.TRANSCRIPT)
                .ifPresent(index -> deleteIndexes(List.of(index)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteTeamKnowledge(Long teamId) {
        deleteIndexes(knowledgeIndexRepository.findAllByTeam_TeamId(teamId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProjectKnowledge(Long projectId) {
        deleteIndexes(knowledgeIndexRepository.findAllByTeam_Project_ProjectId(projectId));
    }

    private void deleteIndexes(List<QaKnowledgeIndex> indexes) {
        for (QaKnowledgeIndex index : indexes) {
            List<String> documentIds = documentFactory.documentIds(
                    index.getSourceId(),
                    index.getSourceType(),
                    0,
                    index.getChunkCount()
            );
            if (!documentIds.isEmpty()) {
                vectorStore.delete(documentIds);
            }
        }

        knowledgeIndexRepository.deleteAllInBatch(indexes);
        if (!indexes.isEmpty()) {
            log.info("[KnowledgeDeletion] Pinecone 청크 및 색인 상태 삭제 완료: sourceCount={}", indexes.size());
        }
    }
}
