package com._penLearning.Noddi.domain.notification.repository;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    /**
     * 알림함 목록을 최신 알림 발생 순서로 조회한다.
     *
     * 숨김 처리한 알림은 사용자 알림함에 다시 노출하지 않는다.
     */
    @Query(
            value = """
                    SELECT notification
                    FROM Notification notification
                    WHERE notification.user.userId = :userId
                      AND notification.hidden = false
                    ORDER BY notification.occurredAt DESC,
                             notification.notificationId DESC
                    """,
            countQuery = """
                    SELECT COUNT(notification)
                    FROM Notification notification
                    WHERE notification.user.userId = :userId
                      AND notification.hidden = false
                    """
    )
    Page<Notification> findVisibleNotifications(
            @Param("userId") Long userId,
            Pageable pageable
    );

    /**
     * 헤더 알림 배지에 표시할 안 읽은 알림 개수를 조회한다.
     */
    @Query("""
            SELECT COUNT(notification)
            FROM Notification notification
            WHERE notification.user.userId = :userId
              AND notification.read = false
              AND notification.hidden = false
            """)
    long countUnreadNotifications(
            @Param("userId") Long userId
    );

    /**
     * 읽음 또는 숨김 처리할 알림을 조회한다.
     *
     * userId까지 조건에 포함해 다른 사용자의 알림을 변경하지 못하게 한다.
     */
    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.notificationId = :notificationId
              AND notification.user.userId = :userId
            """)
    Optional<Notification> findOwnedNotification(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId
    );

    /**
     * 같은 사용자와 원본 리소스에 대해 아직 읽지 않은 동일 유형 알림을 찾는다.
     *
     * 답변 수정 알림이 이미 존재하면 새 레코드를 만들지 않고 이 알림을 갱신한다.
     */
    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.user.userId = :userId
              AND notification.type = :type
              AND notification.referenceType = :referenceType
              AND notification.referenceId = :referenceId
              AND notification.read = false
              AND notification.hidden = false
            ORDER BY notification.occurredAt DESC
            """)
    List<Notification> findUnreadNotificationsByReference(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("referenceType") NotificationReferenceType referenceType,
            @Param("referenceId") Long referenceId,
            Pageable pageable
    );

    /**
     * 동일 참조의 안 읽은 알림 중 가장 최근 알림 하나만 반환한다.
     */
    default Optional<Notification> findLatestUnreadNotification(
            Long userId,
            NotificationType type,
            NotificationReferenceType referenceType,
            Long referenceId
    ) {
        return findUnreadNotificationsByReference(
                userId,
                type,
                referenceType,
                referenceId,
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    /**
     * 묶음 자세히보기를 눌렀을 때 함께 읽음 처리할 Q&A 알림을 조회한다.
     */
    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.user.userId = :userId
              AND notification.projectId = :projectId
              AND notification.teamId = :teamId
              AND notification.type = :type
              AND notification.read = false
              AND notification.hidden = false
            """)
    List<Notification> findUnreadGroup(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("teamId") Long teamId,
            @Param("type") NotificationType type
    );

    /**
     * 묶음 X 버튼 처리 시 같은 읽음 상태의 알림을 모두 조회한다.
     *
     * ALL 필터에서는 같은 팀의 읽음 묶음과 안 읽은 묶음이 따로 표시되므로
     * read 조건이 반드시 필요하다.
     */
    @Query("""
        SELECT notification
        FROM Notification notification
        WHERE notification.user.userId = :userId
          AND notification.projectId = :projectId
          AND notification.teamId = :teamId
          AND notification.type = :type
          AND notification.read = :read
          AND notification.hidden = false
        """)
    List<Notification> findVisibleGroupByReadStatus(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("teamId") Long teamId,
            @Param("type") NotificationType type,
            @Param("read") boolean read
    );

    /**
     * 질문 상태가 변경돼 더 이상 유효하지 않은 알림을 찾는다.
     *
     * 사용자 한 명의 요청이 아니라 시스템 상태 전환에 따른 정리이므로
     * userId 조건 없이 해당 질문을 참조하는 모든 사용자의 알림을 조회한다.
     */
    @Query("""
        SELECT notification
        FROM Notification notification
        WHERE notification.referenceType = :referenceType
          AND notification.referenceId = :referenceId
          AND notification.type IN :types
          AND notification.hidden = false
        """)
    List<Notification> findVisibleNotificationsByReferenceAndTypes(
            @Param("referenceType")
            NotificationReferenceType referenceType,

            @Param("referenceId")
            Long referenceId,

            @Param("types")
            Set<NotificationType> types
    );
}