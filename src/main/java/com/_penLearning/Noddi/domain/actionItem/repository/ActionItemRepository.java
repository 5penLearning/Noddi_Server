package com._penLearning.Noddi.domain.actionItem.repository;

import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByMeetingSummary_SummaryId(Long summaryId);
    List<ActionItem> findByAssignee_UserId(Long userId);

    /**
     * 회의록 상세 화면에 표시할 ActionItem을 연관 데이터와 함께 조회
     */
    @Query("""
        SELECT ai
        FROM ActionItem ai
        JOIN FETCH ai.meetingSummary ms
        JOIN FETCH ms.meeting m
        LEFT JOIN FETCH ai.assignee
        WHERE ms.summaryId = :summaryId
        ORDER BY ai.actionItemId ASC
        """)
    List<ActionItem> findAllBySummaryIdWithDetails(
            @Param("summaryId") Long summaryId
    );

    /**
     * 현재 사용자가 담당자인 ActionItem을 회의 정보와 함께 조회
     */
    @Query("""
        SELECT ai
        FROM ActionItem ai
        JOIN FETCH ai.meetingSummary ms
        JOIN FETCH ms.meeting m
        JOIN FETCH ai.assignee a
        WHERE a.userId = :userId
        ORDER BY ai.dueDate ASC, ai.actionItemId ASC
        """)
    List<ActionItem> findAllByAssigneeIdWithDetails(
            @Param("userId") Long userId
    );
}
