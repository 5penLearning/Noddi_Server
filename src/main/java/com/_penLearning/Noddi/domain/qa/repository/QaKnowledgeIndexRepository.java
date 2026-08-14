package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaKnowledgeIndex;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QaKnowledgeIndexRepository extends JpaRepository<QaKnowledgeIndex, Long> {

    Optional<QaKnowledgeIndex> findBySourceIdAndSourceType(
            Long sourceId,
            SourceType sourceType
    );

    List<QaKnowledgeIndex> findAllByTeam_TeamId(Long teamId);

    List<QaKnowledgeIndex> findAllByTeam_Project_ProjectId(Long projectId);
}
