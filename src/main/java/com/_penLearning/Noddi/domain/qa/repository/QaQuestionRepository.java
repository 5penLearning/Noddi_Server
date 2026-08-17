package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QaQuestionRepository extends JpaRepository<QaQuestion, Long> {
    // 동시성 제어를 위한 비관적 쓰기 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QaQuestion q WHERE q.questionId = :questionId")
    Optional<QaQuestion> findByIdWithLock(@Param("questionId") Long questionId);

    // 질문 목록 페이징 적용 (List -> Page)
    @Query(
            value = """
        SELECT q
        FROM QaQuestion q
        JOIN FETCH q.targetTeam
        JOIN FETCH q.questioner
        WHERE q.questioner = :questioner
        ORDER BY q.createdAt DESC
        """,
            countQuery = """
        SELECT COUNT(q)
        FROM QaQuestion q
        WHERE q.questioner = :questioner
        """
    )
    Page<QaQuestion> findAllByQuestionerWithTeam(
            @Param("questioner") User questioner,
            Pageable pageable
    );

    // 특정 팀에 들어온 질문 목록 조회 (N+1 방지)
    @Query(
            value = """
        SELECT q
        FROM QaQuestion q
        JOIN FETCH q.questioner
        WHERE q.targetTeam = :targetTeam
        ORDER BY q.createdAt DESC
        """,
            countQuery = """
        SELECT COUNT(q)
        FROM QaQuestion q
        WHERE q.targetTeam = :targetTeam
        """
    )
    Page<QaQuestion> findAllByTargetTeamWithUser(@Param("targetTeam") Team targetTeam, Pageable pageable);

    // 단건 상세 조회 (팀 정보 포함)
    @Query("SELECT q FROM QaQuestion q JOIN FETCH q.targetTeam WHERE q.questionId = :questionId")
    Optional<QaQuestion> findByIdWithTeam(@Param("questionId") Long questionId);

    List<QaQuestion> findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            Collection<QaStatus> statuses,
            LocalDateTime threshold
    );

    @Query("""
        SELECT q
        FROM QaQuestion q
        JOIN FETCH q.questioner
        WHERE q.targetTeam = :targetTeam
          AND (:cursor IS NULL OR q.questionId < :cursor)
        ORDER BY q.questionId DESC
        """)
    List<QaQuestion> findFeedByTargetTeam(
            @Param("targetTeam") Team targetTeam,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM QaQuestion question WHERE question.targetTeam = :team")
    void bulkDeleteByTeam(@Param("team") Team team);
}
