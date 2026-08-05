package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaAnswerFeedbackRepository extends JpaRepository<QaAnswerFeedback, Long> {
    List<QaAnswerFeedback> findByAnswer_AnswerId(Long answerId);
    boolean existsByAnswer_AnswerIdAndUser_UserId(Long answerId, Long userId);
}
