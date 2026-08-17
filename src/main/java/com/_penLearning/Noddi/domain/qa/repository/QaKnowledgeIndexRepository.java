package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaKnowledgeIndex;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QaKnowledgeIndexRepository extends JpaRepository<QaKnowledgeIndex, Long> {

    Optional<QaKnowledgeIndex> findBySourceIdAndSourceType(
            Long sourceId,
            SourceType sourceType
    );

    List<QaKnowledgeIndex> findAllByTeam_TeamId(Long teamId);

    List<QaKnowledgeIndex> findAllByTeam_Project_ProjectId(Long projectId);

    /** 원본 공유페이지는 삭제됐지만 TEAM_TEXT 색인 상태가 남은 sourceId를 조회한다. */
    @Query("""
            SELECT knowledgeIndex.sourceId
            FROM QaKnowledgeIndex knowledgeIndex
            WHERE knowledgeIndex.sourceType = :sourceType
              AND NOT EXISTS (
                  SELECT page.pageId
                  FROM TeamPage page
                  WHERE page.pageId = knowledgeIndex.sourceId
              )
            ORDER BY knowledgeIndex.sourceId ASC
            """)
    List<Long> findOrphanedSourceIds(
            @Param("sourceType") SourceType sourceType,
            Pageable pageable
    );
}
