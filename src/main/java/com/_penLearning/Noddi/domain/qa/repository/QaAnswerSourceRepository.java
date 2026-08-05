package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaAnswerSourceRepository extends JpaRepository<QaAnswerSource, Long> {
    List<QaAnswerSource> findByAnswer_AnswerId(Long answerId);
}
