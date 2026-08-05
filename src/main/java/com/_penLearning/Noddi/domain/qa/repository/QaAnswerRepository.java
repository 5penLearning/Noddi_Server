package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QaAnswerRepository extends JpaRepository<QaAnswer, Long> {
    Optional<QaAnswer> findByQuestion_QuestionId(Long questionId);
}
