package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QaAnswerSourceRepository extends JpaRepository<QaAnswerSource, Long> {
    List<QaAnswerSource> findByAnswer_AnswerId(Long answerId);

    List<QaAnswerSource> findByAnswer_AnswerIdOrderByCitationIndexAsc(Long answerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM QaAnswerSource source WHERE source.answer.answerId = :answerId")
    int deleteAllByAnswerId(@Param("answerId") Long answerId);

    @Query("""
        SELECT source
        FROM QaAnswerSource source
        JOIN FETCH source.answer answer
        WHERE answer IN :answers
        ORDER BY answer.answerId ASC, source.citationIndex ASC
        """)
    List<QaAnswerSource> findAllByAnswersOrderByCitationIndex(
            @Param("answers") Collection<QaAnswer> answers
    );
}
