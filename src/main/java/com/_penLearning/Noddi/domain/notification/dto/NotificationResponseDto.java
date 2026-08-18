package com._penLearning.Noddi.domain.notification.dto;

import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationResponseDto {

    /**
     * 알림함 목록 전체 응답이다.
     *
     * totalElements는 DB의 Notification 행 개수가 아니라
     * 묶음 처리를 마친 뒤 화면에 표시되는 항목 개수다.
     */
    @Getter
    @Builder
    public static class NotificationList {

        private List<NotificationItem> items;

        private int page;
        private int size;

        private long totalElements;
        private int totalPages;
        private boolean hasNext;

        // 헤더 배지에 표시할 실제 안 읽은 Notification 개수
        private long unreadCount;
    }

    /**
     * 알림함에 표시되는 항목 하나다.
     *
     * 개별 알림일 수도 있고 여러 알림을 묶은 항목일 수도 있다.
     */
    @Getter
    @Builder
    public static class NotificationItem {

        /*
         * 개별 알림이면 notificationId가 존재한다.
         * 묶음 알림은 특정 알림 하나를 대표하지 않으므로 null이다.
         */
        private Long notificationId;

        /*
         * 묶음 항목을 프론트 목록 key로 사용할 값이다.
         * 개별 알림에서는 null이다.
         *
         * 예:
         * QA_AI_REVIEW_REQUIRED:1:2:false
         */
        private String groupKey;

        private NotificationType type;
        private String message;

        private boolean read;
        private boolean grouped;

        // 개별 알림은 1, 묶음 알림은 포함된 알림 개수
        private long count;

        /*
         * 개별 알림은 자기 자신의 발생 시각,
         * 묶음 알림은 그룹에서 가장 최근 알림의 발생 시각이다.
         */
        private LocalDateTime occurredAt;

        private Navigation navigation;
    }

    /**
     * 자세히보기 클릭 시 프론트 라우팅에 사용할 데이터
     */
    @Getter
    @Builder
    public static class Navigation {

        private NotificationNavigationType type;

        private Long projectId;
        private Long teamId;

        /*
         * QA_QUESTION이면 questionId,
         * TEAM_INVITE이면 teamInviteId,
         * PROJECT_INVITE이면 projectInviteId다.
         *
         * QA_TEAM_FEED는 여러 질문을 묶으므로 null이다.
         */
        private Long referenceId;
    }
}