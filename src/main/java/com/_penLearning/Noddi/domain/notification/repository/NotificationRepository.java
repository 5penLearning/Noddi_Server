package com._penLearning.Noddi.domain.notification.repository;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림함을 조회한다.
     *
     * 숨긴 알림은 제외하고, 갱신된 알림이 위로 올라오도록 updatedAt 역순으로 정렬한다.
     * notificationId는 같은 시각에 생성된 알림의 정렬 순서를 고정하기 위한 보조 조건이다.
     */
    Page<Notification>
    findByUser_UserIdAndHiddenFalseOrderByOccurredAtDescNotificationIdDesc(
            Long userId,
            Pageable pageable
    );

    /**
     * 헤더의 빨간 알림 배지에 표시할 안 읽은 알림 개수다.
     */
    long countByUser_UserIdAndReadFalseAndHiddenFalse(Long userId);

    /**
     * 개별 읽음 또는 숨김 처리 시
     * 다른 사용자의 알림을 변경하지 못하도록 알림 ID와 사용자 ID를 함께 조회한다.
     */
    Optional<Notification> findByNotificationIdAndUser_UserId(
            Long notificationId,
            Long userId
    );

    /**
     * 동일 답변의 안 읽은 수정 알림을 찾는다.
     *
     * 이미 존재하면 새 알림을 추가하지 않고 기존 알림의 메시지를 갱신한다.
     */
    Optional<Notification>
    findFirstByUser_UserIdAndTypeAndReferenceTypeAndReferenceIdAndReadFalseAndHiddenFalseOrderByUpdatedAtDesc(
            Long userId,
            NotificationType type,
            NotificationReferenceType referenceType,
            Long referenceId
    );

    /**
     * 알림함에서 묶음 자세히보기를 눌렀을 때
     * 같은 프로젝트·팀·알림 종류에 속한 안 읽은 알림을 한 번에 읽음 처리한다.
     */
    List<Notification>
    findAllByUser_UserIdAndProjectIdAndTeamIdAndTypeAndReadFalseAndHiddenFalse(
            Long userId,
            Long projectId,
            Long teamId,
            NotificationType type
    );
}