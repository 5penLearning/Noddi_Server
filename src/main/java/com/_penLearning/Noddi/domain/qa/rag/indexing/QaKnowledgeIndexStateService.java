package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.QaKnowledgeIndex;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.repository.QaKnowledgeIndexRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 지식 소스의 색인 상태를 짧은 DB 트랜잭션으로 조회·갱신한다.
 * Pinecone 호출은 각 소스의 색인 서비스가 트랜잭션 밖에서 담당한다.
 */
@Service
@RequiredArgsConstructor
public class QaKnowledgeIndexStateService {

    private final QaKnowledgeIndexRepository qaKnowledgeIndexRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Optional<KnowledgeIndexState> get(Long sourceId, SourceType sourceType) {
        return qaKnowledgeIndexRepository
                .findBySourceIdAndSourceType(sourceId, sourceType)
                .map(index -> new KnowledgeIndexState(index.getContentHash(), index.getChunkCount()));
    }

    @Transactional
    public void recordSuccess(
            Long sourceId,
            Long teamId,
            SourceType sourceType,
            String contentHash,
            int chunkCount
    ) {
        QaKnowledgeIndex index = qaKnowledgeIndexRepository
                .findBySourceIdAndSourceType(sourceId, sourceType)
                .orElseGet(() -> new QaKnowledgeIndex(
                        getTeam(teamId),
                        sourceId,
                        sourceType,
                        contentHash,
                        chunkCount
                ));

        index.update(contentHash, chunkCount);
        qaKnowledgeIndexRepository.save(index);
    }

    @Transactional
    public void remove(Long sourceId, SourceType sourceType) {
        qaKnowledgeIndexRepository
                .findBySourceIdAndSourceType(sourceId, sourceType)
                .ifPresent(qaKnowledgeIndexRepository::delete);
    }

    private Team getTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
    }
}
