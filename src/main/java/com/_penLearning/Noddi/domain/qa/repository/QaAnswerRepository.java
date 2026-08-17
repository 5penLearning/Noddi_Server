package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QaAnswerRepository extends JpaRepository<QaAnswer, Long> {
    // 이미 답변이 존재하는지 검증
    boolean existsByQuestion(QaQuestion question);

    // 특정 질문의 답변 조회
    Optional<QaAnswer> findByQuestion(QaQuestion question);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM QaAnswer a
            JOIN FETCH a.question q
            JOIN FETCH q.targetTeam
            WHERE a.answerId = :answerId
            """)
    Optional<QaAnswer> findByIdWithQuestionAndTeamForUpdate(@Param("answerId") Long answerId);

    @Query("""
        SELECT a
        FROM QaAnswer a
        JOIN FETCH a.question q
        LEFT JOIN FETCH a.revisedBy
        WHERE q IN :questions
        """)
    List<QaAnswer> findAllByQuestionsWithReviser(
            @Param("questions") Collection<QaQuestion> questions
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM QaAnswer answer WHERE answer.question.targetTeam = :team")
    void bulkDeleteByTeam(@Param("team") Team team);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM QaAnswer answer WHERE answer.question.targetTeam.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);

    /**
     * 수정 이력 조회에 필요한 답변, 질문, 대상 팀, 프로젝트를 함께 조회한다.
     *
     * 서비스에서 프로젝트 구성원 권한을 검사할 때
     * 연관 엔티티가 각각 추가 조회되는 것을 방지한다.
     */
    @Query("""
        SELECT answer
        FROM QaAnswer answer
        JOIN FETCH answer.question question
        JOIN FETCH question.targetTeam targetTeam
        JOIN FETCH targetTeam.project
        WHERE answer.answerId = :answerId
        """)
    Optional<QaAnswer> findByIdWithQuestionTeamAndProject(
            @Param("answerId") Long answerId
    );
}
