package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QaAnswerRevisionRepository
        extends JpaRepository<QaAnswerRevision, Long> {

    /**
     * AI 최초 답변의 버전 1이 이미 저장됐는지 확인한다.
     *
     * 질문 생성 이벤트가 중복 처리되더라도 최초 버전이
     * 중복으로 생성되지 않게 방어하는 용도로 사용한다.
     */
    boolean existsByAnswer(QaAnswer answer);

    /**
     * 마지막 버전을 조회한다.
     *
     * 담당자 수정 시 마지막 버전 번호에 1을 더해
     * 다음 버전 번호를 계산한다.
     */
    Optional<QaAnswerRevision> findTopByAnswerOrderByVersionNumberDesc(QaAnswer answer);

    /**
     * 답변의 모든 버전을 오래된 순서로 조회한다.
     *
     * HUMAN 버전에서 수정자 이름을 응답해야 하므로
     * revisedBy를 LEFT JOIN FETCH로 함께 조회한다.
     *
     * AI 버전은 revisedBy가 null이기 때문에 일반 JOIN이 아닌
     * LEFT JOIN을 사용해야 조회 결과에서 제외되지 않는다.
     */
    @Query("""
            SELECT revision
            FROM QaAnswerRevision revision
            LEFT JOIN FETCH revision.revisedBy
            WHERE revision.answer = :answer
            ORDER BY revision.versionNumber ASC
            """)
    List<QaAnswerRevision> findAllByAnswerWithReviserOrderByVersionNumberAsc(
            @Param("answer") QaAnswer answer
    );
}