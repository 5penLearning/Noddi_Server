package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QaAnswerRepository extends JpaRepository<QaAnswer, Long> {
    // 이미 답변이 존재하는지 검증
    boolean existsByQuestion(QaQuestion question);

    // 특정 질문의 답변 조회
    Optional<QaAnswer> findByQuestion(QaQuestion question);
}
