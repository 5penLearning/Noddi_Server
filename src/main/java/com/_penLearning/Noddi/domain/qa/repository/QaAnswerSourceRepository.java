package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QaAnswerSourceRepository extends JpaRepository<QaAnswerSource, Long> {
    List<QaAnswerSource> findByAnswer_AnswerId(Long answerId);

    List<QaAnswerSource> findByAnswer_AnswerIdOrderByCitationIndexAsc(Long answerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM QaAnswerSource source WHERE source.answer.answerId = :answerId")
    int deleteAllByAnswerId(@Param("answerId") Long answerId);
}
