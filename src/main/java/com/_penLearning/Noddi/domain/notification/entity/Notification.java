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

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean hidden;

    @Column(nullable = false)
    private boolean read;

    @Builder
    public Notification(User user, NotificationType type, NotificationReferenceType referenceType, Long referenceId, String message) {
        this.user = user;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.message = message;
        this.hidden = false;
        this.read = false;
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
    }
}
