package com._penLearning.Noddi.domain.home.dto;

import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class HomeAiAnswerResponseDto {

    /** 홈 프로젝트 탭에 표시할 프로젝트명과 미확인 AI 답변 개수다. */
    @Getter
    @Builder
    public static class ProjectStatus {

        private Long projectId;
        private String projectName;
        private long unreadAnswerCount;

        public static ProjectStatus of(
                HomeProjectProjection project,
                long unreadAnswerCount
        ) {
            return ProjectStatus.builder()
                    .projectId(project.getProjectId())
                    .projectName(project.getProjectName())
                    .unreadAnswerCount(unreadAnswerCount)
                    .build();
        }
    }

    /** 선택한 프로젝트의 카드 목록과 페이지 정보를 함께 반환한다. */
    @Getter
    @Builder
    public static class CardPage {

        private List<Card> items;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /** 와이어프레임에서 한 장으로 표시할 미확인 AI 답변 카드다. */
    @Getter
    @Builder
    public static class Card {

        private Long notificationId;
        private Long questionId;

        private Questioner questioner;
        private String questionContent;
        private LocalDateTime questionCreatedAt;

        private Long answerId;
        private String answerContent;

        private Long projectId;
        private String projectName;
        private Long teamId;
        private String teamName;

        private List<Source> sources;

        public static Card of(
                Notification notification,
                QaAnswer answer,
                List<QaAnswerSource> sources
        ) {
            QaQuestion question = answer.getQuestion();
            Team team = question.getTargetTeam();

            return Card.builder()
                    .notificationId(notification.getNotificationId())
                    .questionId(question.getQuestionId())
                    .questioner(Questioner.from(question.getQuestioner()))
                    .questionContent(question.getContent())
                    .questionCreatedAt(question.getCreatedAt())
                    .answerId(answer.getAnswerId())
                    .answerContent(answer.getContent())
                    .projectId(team.getProject().getProjectId())
                    .projectName(team.getProject().getName())
                    .teamId(team.getTeamId())
                    .teamName(team.getName())
                    .sources(sources.stream()
                            .map(source -> Source.from(
                                    source,
                                    team.getTeamId()
                            ))
                            .toList())
                    .build();
        }
    }

    /** AI 답변이 실제로 인용한 회의 전사 또는 팀 공유 페이지다. */
    @Getter
    @Builder
    public static class Source {

        private int citationIndex;
        private SourceType sourceType;
        private Long referenceId;
        private String sourceTitle;
        private String excerpt;
        private SourceNavigation navigation;

        public static Source from(
                QaAnswerSource source,
                Long targetTeamId
        ) {
            return Source.builder()
                    .citationIndex(source.getCitationIndex())
                    .sourceType(source.getSourceType())
                    .referenceId(source.getReferenceId())
                    .sourceTitle(source.getSourceTitle())
                    .excerpt(source.getExcerpt())
                    .navigation(SourceNavigation.from(
                            source,
                            targetTeamId
                    ))
                    .build();
        }
    }

    /** 출처 종류별 상세 화면 이동에 필요한 식별자를 구조화한다. */
    @Getter
    @Builder
    public static class SourceNavigation {

        private HomeAiAnswerSourceNavigationType type;
        private Long meetingId;
        private Long teamId;
        private Long pageId;

        public static SourceNavigation from(
                QaAnswerSource source,
                Long targetTeamId
        ) {
            return switch (source.getSourceType()) {
                case TRANSCRIPT -> SourceNavigation.builder()
                        .type(HomeAiAnswerSourceNavigationType.MEETING_SUMMARY)
                        .meetingId(source.getReferenceId())
                        .build();

                case TEAM_TEXT -> SourceNavigation.builder()
                        .type(HomeAiAnswerSourceNavigationType.TEAM_PAGE)
                        .teamId(targetTeamId)
                        .pageId(source.getReferenceId())
                        .build();
            };
        }
    }

    /** 카드 상단에 표시할 질문자의 공개 프로필 정보다. */
    @Getter
    @Builder
    public static class Questioner {

        private Long userId;
        private String name;
        private String department;
        private String position;
        private String profileImageUrl;

        public static Questioner from(User user) {
            return Questioner.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .department(user.getDepartment())
                    .position(user.getPosition())
                    .profileImageUrl(profileImageUrl(user))
                    .build();
        }

        private static String profileImageUrl(User user) {
            if (user.getProfileImageKey() == null) {
                return null;
            }

            return "/api/v1/users/" + user.getUserId()
                    + "/profile-image?v=" + user.getProfileImageKey();
        }
    }
}
