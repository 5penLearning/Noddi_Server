package com._penLearning.Noddi.domain.actionItem.repository;

import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    /**
     * 회의록 상세 화면에 표시할 ActionItem을 연관 데이터와 함께 조회
     */
    @Query("""
        SELECT ai
        FROM ActionItem ai
        JOIN FETCH ai.meeting m
        LEFT JOIN FETCH ai.assignee
        WHERE m.meetingId = :meetingId
        ORDER BY ai.actionItemId ASC
        """)
    List<ActionItem> findAllByMeetingIdWithDetails(
            @Param("meetingId") Long meetingId
    );

    /**
     * 현재 사용자가 담당자인 ActionItem을 회의 정보와 함께 조회
     * ActionItem이 속한 팀에 현재도 가입된 경우만 조회
     */
    @Query("""
    SELECT ai
    FROM ActionItem ai
    JOIN FETCH ai.meeting m
    JOIN FETCH ai.assignee a
    WHERE a.userId = :userId
      AND EXISTS (
          SELECT 1
          FROM TeamMember tm
          WHERE tm.team = m.team
            AND tm.user = a
      )
    ORDER BY ai.dueDate ASC, ai.actionItemId ASC
    """)
    List<ActionItem> findAllByAssigneeIdWithDetails(
            @Param("userId") Long userId
    );
}
