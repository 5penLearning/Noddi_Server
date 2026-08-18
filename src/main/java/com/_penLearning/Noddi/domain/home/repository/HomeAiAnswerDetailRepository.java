package com._penLearning.Noddi.domain.home.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 홈 카드 조립에 필요한 Q&A 상세 데이터를 일괄 조회한다. */
public interface HomeAiAnswerDetailRepository
        extends Repository<QaAnswer, Long> {

    /**
     * 현재 답변과 카드에 필요한 모든 단일 연관관계를 한 번에 조회한다.
     *
     * 출처처럼 컬렉션인 연관관계는 여기서 fetch join하지 않는다.
     * 컬렉션 fetch join은 결과 행을 증가시켜 페이징 및 중복 처리에 불리하기 때문이다.
     */
    @Query("""
            SELECT answer
            FROM QaAnswer answer
            JOIN FETCH answer.question question
            JOIN FETCH question.questioner
            JOIN FETCH question.targetTeam team
            JOIN FETCH team.project
            LEFT JOIN FETCH answer.revisedBy
            WHERE question.questionId IN :questionIds
            """)
    List<QaAnswer> findAllCardDetailsByQuestionIds(
            @Param("questionIds") Collection<Long> questionIds
    );
}
