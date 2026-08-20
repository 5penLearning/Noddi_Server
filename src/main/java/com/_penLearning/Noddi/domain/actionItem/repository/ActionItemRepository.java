package com._penLearning.Noddi.domain.actionItem.repository;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * 마이페이지에 표시할 현재 사용자의 ActionItem을 회의 정보와 함께 조회한다.
     * 전달받은 진행 상태에 해당하고, ActionItem이 속한 팀에 현재도 가입된 경우만 조회한다.
     */
    @Query("""
    SELECT ai
    FROM ActionItem ai
    JOIN FETCH ai.meeting m
    JOIN FETCH ai.assignee a
    WHERE a.userId = :userId
      AND ai.status IN :statuses
      AND EXISTS (
          SELECT 1
          FROM TeamMember tm
          WHERE tm.team = m.team
            AND tm.user = a
      )
    ORDER BY CASE WHEN ai.dueDate IS NULL THEN 1 ELSE 0 END ASC,
             ai.dueDate ASC,
             ai.actionItemId ASC
    """)
    List<ActionItem> findAllByAssigneeIdWithDetails(
            @Param("userId") Long userId,
            @Param("statuses") List<ActionItemStatus> statuses
    );

    /**
     * 팀별 개인 To-do 응답 조립에 필요한 진행 중 ActionItem과
     * Meeting·Team·Project를 일괄 조회한다.
     * 기존 개인 목록 API의 전체 마감일순 계약과 분리해 프로젝트·팀별로 정렬한다.
     */
    @Query("""
    SELECT ai
    FROM ActionItem ai
    JOIN FETCH ai.meeting m
    JOIN FETCH m.team t
    JOIN FETCH t.project
    JOIN FETCH ai.assignee a
    WHERE a.userId = :userId
      AND ai.status IN :statuses
      AND EXISTS (
          SELECT 1
          FROM TeamMember tm
          WHERE tm.team = t
            AND tm.user = a
      )
    ORDER BY t.project.projectId ASC,
             t.teamId ASC,
             CASE WHEN ai.dueDate IS NULL THEN 1 ELSE 0 END ASC,
             ai.dueDate ASC,
             ai.actionItemId ASC
    """)
    List<ActionItem> findAllByAssigneeIdWithTeamDetails(
            @Param("userId") Long userId,
            @Param("statuses") List<ActionItemStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ActionItem actionItem WHERE actionItem.meeting.team = :team")
    void bulkDeleteByTeam(@Param("team") Team team);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ActionItem actionItem WHERE actionItem.meeting.team.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);
}
