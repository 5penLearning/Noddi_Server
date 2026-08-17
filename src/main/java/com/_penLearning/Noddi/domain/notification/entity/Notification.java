package com._penLearning.Noddi.domain.notification.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Notification")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * 자세히보기를 눌렀을 때 이동할 리소스의 종류다.
     *
     * referenceType과 referenceId는 항상 한 쌍으로 사용한다.
     * 예: QA_QUESTION + questionId
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationReferenceType referenceType;

    @Column(nullable = false)
    private Long referenceId;

    /**
     * 알림이 속한 프로젝트다.
     * 프로젝트별 알림 묶음과 프론트 라우팅에 사용한다.
     */
    private Long projectId;

    /**
     * 알림이 속한 팀이다.
     * Q&A 알림은 대상 팀 ID를 저장한다.
     */
    private Long teamId;

    @Column(nullable = false)
    private String message;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    public Notification(User user, NotificationType type, NotificationReferenceType referenceType, Long referenceId, Long projectId, Long teamId, String message) {
        this.user = user;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.projectId = projectId;
        this.teamId = teamId;
        this.message = message;
        this.hidden = false;
        this.read = false;
        this.occurredAt = LocalDateTime.now();
    }

    public void markAsRead() {

        this.read = true;
    }

    public void hide() {
        this.read = true;
        this.hidden = true;
    }

    public void updateMessage(String message) {
        this.message = message;
        this.occurredAt = LocalDateTime.now();
    }
}
