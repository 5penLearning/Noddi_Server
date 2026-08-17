package com._penLearning.Noddi.domain.notification.service;

import com._penLearning.Noddi.domain.notification.dto.NotificationFilter;
import com._penLearning.Noddi.domain.notification.dto.NotificationNavigationType;
import com._penLearning.Noddi.domain.notification.dto.NotificationResponseDto;
import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.repository.NotificationRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final int MAX_STORED_NOTIFICATIONS = 500;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final TeamRepository teamRepository;
    private final NotificationMessageFactory notificationMessageFactory;

    /**
     * 사용자의 알림함을 조회한다.
     *
     * 저장된 Notification 행을 바로 반환하지 않고 다음 순서로 가공한다.
     *
     * 1. 숨기지 않은 최근 알림을 최대 500개 조회
     * 2. ALL 또는 UNREAD 필터 적용
     * 3. AI 답변 검토 요청 알림 그룹화
     * 4. 최신 발생 시각 순으로 정렬
     * 5. 화면 표시 항목 기준으로 페이지 분할
     */
    public NotificationResponseDto.NotificationList getNotifications(
            Long userId,
            NotificationFilter filter,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        /*
         * 그룹화하기 전 DB 페이지를 그대로 프론트 페이지로 사용할 수 없다.
         *
         * 예를 들어 DB 알림 5개가 화면에서는 묶음 알림 1개가 될 수 있으므로
         * 보관 정책의 최대 개수인 500개까지 가져온 뒤 화면 항목으로 변환한다.
         */
        Page<Notification> notificationPage =
                notificationRepository.findVisibleNotifications(
                        userId,
                        PageRequest.of(0, MAX_STORED_NOTIFICATIONS)
                );

        List<Notification> filteredNotifications =
                applyFilter(notificationPage.getContent(), filter);

        List<NotificationResponseDto.NotificationItem> displayItems =
                createDisplayItems(filteredNotifications);

        // 개별 알림과 묶음 알림을 함께 최신 발생 순서로 정렬한다.
        displayItems.sort(
                Comparator.comparing(
                        NotificationResponseDto.NotificationItem::getOccurredAt
                ).reversed()
        );

        List<NotificationResponseDto.NotificationItem> pagedItems =
                paginate(displayItems, page, size);

        long totalElements = displayItems.size();
        int totalPages = calculateTotalPages(totalElements, size);
        boolean hasNext = page + 1 < totalPages;

        /*
         * 배지의 unreadCount는 화면에 표시되는 묶음 개수가 아니라
         * 아직 확인하지 않은 실제 알림 사건의 개수다.
         */
        long unreadCount =
                notificationRepository.countUnreadNotifications(userId);

        return NotificationResponseDto.NotificationList.builder()
                .items(pagedItems)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .unreadCount(unreadCount)
                .build();
    }

    private List<Notification> applyFilter(
            List<Notification> notifications,
            NotificationFilter filter
    ) {
        if (filter == NotificationFilter.ALL) {
            return notifications;
        }

        return notifications.stream()
                .filter(notification -> !notification.isRead())
                .toList();
    }

    /**
     * 개별 알림과 묶음 알림을 화면 표시 항목으로 변환한다.
     *
     * 현재 묶음 대상은 QA_AI_REVIEW_REQUIRED뿐이다.
     * AI 최종 실패는 사용자가 질문별로 직접 답변해야 하므로 묶지 않는다.
     */
    private List<NotificationResponseDto.NotificationItem> createDisplayItems(
            List<Notification> notifications
    ) {
        List<NotificationResponseDto.NotificationItem> items =
                new ArrayList<>();

        Map<ReviewGroupKey, List<Notification>> reviewGroups =
                new LinkedHashMap<>();

        for (Notification notification : notifications) {
            if (notification.getType()
                    == NotificationType.QA_AI_REVIEW_REQUIRED) {

                ReviewGroupKey groupKey = new ReviewGroupKey(
                        notification.getProjectId(),
                        notification.getTeamId(),
                        notification.isRead()
                );

                reviewGroups.computeIfAbsent(
                        groupKey,
                        ignored -> new ArrayList<>()
                ).add(notification);

                continue;
            }

            items.add(createIndividualItem(notification));
        }

        for (Map.Entry<ReviewGroupKey, List<Notification>> entry
                : reviewGroups.entrySet()) {

            items.add(createReviewGroupItem(
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        return items;
    }

    /**
     * 일반 알림 하나를 화면 항목 하나로 변환한다.
     */
    private NotificationResponseDto.NotificationItem createIndividualItem(
            Notification notification
    ) {
        NotificationNavigationType navigationType =
                resolveNavigationType(notification.getReferenceType());

        NotificationResponseDto.Navigation navigation =
                NotificationResponseDto.Navigation.builder()
                        .type(navigationType)
                        .projectId(notification.getProjectId())
                        .teamId(notification.getTeamId())
                        .referenceId(notification.getReferenceId())
                        .build();

        return NotificationResponseDto.NotificationItem.builder()
                .notificationId(notification.getNotificationId())
                .groupKey(null)
                .type(notification.getType())
                .message(notification.getMessage())
                .read(notification.isRead())
                .grouped(false)
                .count(1)
                .occurredAt(notification.getOccurredAt())
                .navigation(navigation)
                .build();
    }

    /**
     * 같은 프로젝트·팀·읽음 상태의 AI 검토 요청을 묶음 항목 하나로 변환한다.
     */
    private NotificationResponseDto.NotificationItem createReviewGroupItem(
            ReviewGroupKey key,
            List<Notification> notifications
    ) {
        /*
         * 그룹에 포함된 알림 중 가장 최근 알림을 찾아
         * 묶음 항목의 발생 시각으로 사용한다.
         */
        Notification latestNotification = notifications.stream()
                .max(Comparator.comparing(Notification::getOccurredAt))
                .orElseThrow();

        String message = createReviewGroupMessage(
                key,
                latestNotification,
                notifications.size()
        );

        String groupKey = NotificationType.QA_AI_REVIEW_REQUIRED
                + ":"
                + key.projectId()
                + ":"
                + key.teamId()
                + ":"
                + key.read();

        NotificationResponseDto.Navigation navigation =
                NotificationResponseDto.Navigation.builder()
                        .type(NotificationNavigationType.QA_TEAM_FEED)
                        .projectId(key.projectId())
                        .teamId(key.teamId())
                        .referenceId(null)
                        .build();

        return NotificationResponseDto.NotificationItem.builder()
                .notificationId(null)
                .groupKey(groupKey)
                .type(NotificationType.QA_AI_REVIEW_REQUIRED)
                .message(message)
                .read(key.read())
                .grouped(true)
                .count(notifications.size())
                .occurredAt(latestNotification.getOccurredAt())
                .navigation(navigation)
                .build();
    }

    /**
     * 묶음 메시지는 현재 프로젝트·팀 이름과 그룹 개수를 사용해 생성한다.
     *
     * 팀이 삭제돼 더 이상 조회되지 않는 예외적인 상황에서는
     * 저장돼 있던 최신 개별 알림 문구를 대신 사용한다.
     */
    private String createReviewGroupMessage(
            ReviewGroupKey key,
            Notification latestNotification,
            long count
    ) {
        return teamRepository.findById(key.teamId())
                .map(team -> notificationMessageFactory
                        .createQaReviewGroupMessage(
                                team.getProject().getName(),
                                team.getName(),
                                count
                        ))
                .orElse(latestNotification.getMessage());
    }

    private NotificationNavigationType resolveNavigationType(
            NotificationReferenceType referenceType
    ) {
        return switch (referenceType) {
            case QA_QUESTION ->
                    NotificationNavigationType.QA_QUESTION;

            case TEAM_INVITE, PROJECT_INVITE ->
                    NotificationNavigationType.MY_INVITATIONS;

            case ACTION_ITEM ->
                    NotificationNavigationType.HOME_ACTION_ITEMS;
        };
    }

    /**
     * 그룹화가 끝난 화면 항목을 page와 size에 맞춰 자른다.
     */
    private List<NotificationResponseDto.NotificationItem> paginate(
            List<NotificationResponseDto.NotificationItem> items,
            int page,
            int size
    ) {
        long requestedStart = (long) page * size;

        if (requestedStart >= items.size()) {
            return List.of();
        }

        int startIndex = (int) requestedStart;
        int endIndex = Math.min(startIndex + size, items.size());

        return List.copyOf(items.subList(startIndex, endIndex));
    }

    private int calculateTotalPages(
            long totalElements,
            int size
    ) {
        if (totalElements == 0) {
            return 0;
        }

        return (int) ((totalElements + size - 1) / size);
    }

    private void validatePageRequest(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size는 1 이상 50 이하여야 합니다."
            );
        }
    }

    /**
     * AI 검토 묶음의 식별 기준이다.
     *
     * 읽은 묶음과 안 읽은 묶음은 읽음 처리 방식이 다르므로
     * read도 그룹 기준에 포함한다.
     */
    private record ReviewGroupKey(
            Long projectId,
            Long teamId,
            boolean read
    ) {
    }
}