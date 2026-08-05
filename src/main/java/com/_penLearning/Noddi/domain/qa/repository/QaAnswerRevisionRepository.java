package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaAnswerRevisionRepository extends JpaRepository<QaAnswerRevision, Long> {
    List<QaAnswerRevision> findByAnswer_AnswerIdOrderByCreatedAtDesc(Long answerId);
}
