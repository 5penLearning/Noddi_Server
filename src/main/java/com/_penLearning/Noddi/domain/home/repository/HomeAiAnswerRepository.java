package com._penLearning.Noddi.domain.home.repository;

import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerCountProjection;
import com._penLearning.Noddi.domain.home.dto.HomeProjectProjection;
import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** 홈 AI 답변 현황에 필요한 읽기 전용 쿼리를 관리한다. */
public interface HomeAiAnswerRepository
        extends Repository<Notification, Long> {

    /**
     * 홈 상단 탭에 표시할 현재 사용자의 모든 참여 프로젝트를 조회한다.
     *
     * 알림 유무와 무관하게 조회하므로 미확인 답변이 0개인 프로젝트도 포함된다.
     */
    @Query("""
            SELECT projectMember.project.projectId AS projectId,
                   projectMember.project.name AS projectName
            FROM ProjectMember projectMember
            WHERE projectMember.user.userId = :userId
            ORDER BY projectMember.project.projectId ASC
            """)
    List<HomeProjectProjection> findProjectsByMemberUserId(
            @Param("userId") Long userId
    );

    /**
     * 현재 사용자가 아직 검토하지 않은 AI 답변 알림을 프로젝트별로 집계한다.
     *
     * Notification의 수신자 조건으로 다른 사용자의 알림을 차단하고,
     * EXISTS 서브쿼리로 사용자가 현재도 알림의 대상 팀에 소속돼 있는지 확인한다.
     */
    @Query("""
            SELECT notification.projectId AS projectId,
                   COUNT(notification.notificationId) AS unreadCount
            FROM Notification notification
            WHERE notification.user.userId = :userId
              AND notification.type = :notificationType
              AND notification.referenceType = :referenceType
              AND notification.read = false
              AND notification.hidden = false
              AND EXISTS (
                    SELECT teamMember.teamMemberId
                    FROM TeamMember teamMember
                    WHERE teamMember.user.userId = :userId
                      AND teamMember.team.teamId = notification.teamId
              )
              AND EXISTS (
                    SELECT answer.answerId
                    FROM QaAnswer answer
                    WHERE answer.question.questionId = notification.referenceId
                      AND answer.question.targetTeam.teamId = notification.teamId
                      AND answer.question.targetTeam.project.projectId = notification.projectId
              )
            GROUP BY notification.projectId
            ORDER BY notification.projectId ASC
            """)
    List<HomeAiAnswerCountProjection> findUnreadAiAnswerCountsByProject(
            @Param("userId") Long userId,
            @Param("notificationType") NotificationType notificationType,
            @Param("referenceType") NotificationReferenceType referenceType
    );

    /** 요청자가 현재 프로젝트 멤버인지 ID만으로 확인한다. */
    @Query("""
            SELECT CASE WHEN COUNT(projectMember) > 0 THEN true ELSE false END
            FROM ProjectMember projectMember
            WHERE projectMember.project.projectId = :projectId
              AND projectMember.user.userId = :userId
            """)
    boolean existsProjectMembership(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );

    /**
     * 선택한 프로젝트의 유효한 미확인 AI 검토 알림을 최신순으로 조회한다.
     *
     * EXISTS 조건은 현재 팀 소속과 질문·답변의 존재를 함께 검증한다.
     * 삭제된 리소스를 참조하는 오래된 알림이 카드로 노출되는 것을 막는다.
     */
    @Query(
            value = """
                    SELECT notification
                    FROM Notification notification
                    WHERE notification.user.userId = :userId
                      AND notification.projectId = :projectId
                      AND notification.type = :notificationType
                      AND notification.referenceType = :referenceType
                      AND notification.read = false
                      AND notification.hidden = false
                      AND EXISTS (
                            SELECT teamMember.teamMemberId
                            FROM TeamMember teamMember
                            WHERE teamMember.user.userId = :userId
                              AND teamMember.team.teamId = notification.teamId
                      )
                      AND EXISTS (
                            SELECT answer.answerId
                            FROM QaAnswer answer
                            WHERE answer.question.questionId = notification.referenceId
                              AND answer.question.targetTeam.teamId = notification.teamId
                              AND answer.question.targetTeam.project.projectId = notification.projectId
                      )
                    ORDER BY notification.occurredAt DESC,
                             notification.notificationId DESC
                    """,
            countQuery = """
                    SELECT COUNT(notification)
                    FROM Notification notification
                    WHERE notification.user.userId = :userId
                      AND notification.projectId = :projectId
                      AND notification.type = :notificationType
                      AND notification.referenceType = :referenceType
                      AND notification.read = false
                      AND notification.hidden = false
                      AND EXISTS (
                            SELECT teamMember.teamMemberId
                            FROM TeamMember teamMember
                            WHERE teamMember.user.userId = :userId
                              AND teamMember.team.teamId = notification.teamId
                      )
                      AND EXISTS (
                            SELECT answer.answerId
                            FROM QaAnswer answer
                            WHERE answer.question.questionId = notification.referenceId
                              AND answer.question.targetTeam.teamId = notification.teamId
                              AND answer.question.targetTeam.project.projectId = notification.projectId
                      )
                    """
    )
    Page<Notification> findUnreadAiAnswerNotifications(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("notificationType") NotificationType notificationType,
            @Param("referenceType") NotificationReferenceType referenceType,
            Pageable pageable
    );
}
