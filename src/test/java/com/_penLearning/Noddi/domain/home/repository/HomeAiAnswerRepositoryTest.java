package com._penLearning.Noddi.domain.home.repository;

import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerCountProjection;
import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerResponseDto;
import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerSourceNavigationType;
import com._penLearning.Noddi.domain.home.service.HomeAiAnswerQueryService;
import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.dto.NotificationFilter;
import com._penLearning.Noddi.domain.notification.dto.NotificationResponseDto;
import com._penLearning.Noddi.domain.notification.message.NotificationMessageFactory;
import com._penLearning.Noddi.domain.notification.service.NotificationCommandService;
import com._penLearning.Noddi.domain.notification.service.NotificationQueryService;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        HomeAiAnswerQueryService.class,
        NotificationCommandService.class,
        NotificationQueryService.class,
        NotificationMessageFactory.class
})
class HomeAiAnswerRepositoryTest {

    @Autowired
    private HomeAiAnswerRepository homeAiAnswerRepository;

    @Autowired
    private HomeAiAnswerDetailRepository homeAiAnswerDetailRepository;

    @Autowired
    private HomeAiAnswerQueryService homeAiAnswerQueryService;

    @Autowired
    private NotificationCommandService notificationCommandService;

    @Autowired
    private NotificationQueryService notificationQueryService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void countsOnlyUnreadReviewNotificationsForTeamsTheUserStillBelongsTo() {
        Organization organization = persistOrganization();
        User currentUser = persistUser(
                organization,
                "reviewer@noddi.test",
                "검토자"
        );
        User otherUser = persistUser(
                organization,
                "other@noddi.test",
                "다른 사용자"
        );

        Project firstProject = persistProject(
                organization,
                currentUser,
                "첫 번째 프로젝트"
        );
        Project secondProject = persistProject(
                organization,
                currentUser,
                "두 번째 프로젝트"
        );
        Project noNotificationProject = persistProject(
                organization,
                currentUser,
                "알림 없는 프로젝트"
        );
        Project projectWithoutTeam = persistProject(
                organization,
                currentUser,
                "소속 팀 없는 프로젝트"
        );

        persistProjectMember(firstProject, currentUser);
        persistProjectMember(secondProject, currentUser);
        persistProjectMember(noNotificationProject, currentUser);
        persistProjectMember(projectWithoutTeam, currentUser);

        Team firstTeam = persistTeam(firstProject, currentUser, "첫 번째 팀");
        Team anotherFirstProjectTeam = persistTeam(
                firstProject,
                currentUser,
                "첫 번째 프로젝트의 다른 팀"
        );
        Team secondTeam = persistTeam(secondProject, currentUser, "두 번째 팀");
        Team noNotificationTeam = persistTeam(
                noNotificationProject,
                currentUser,
                "알림 없는 팀"
        );
        Team formerTeam = persistTeam(firstProject, currentUser, "탈퇴한 팀");

        persistTeamMember(firstTeam, currentUser);
        persistTeamMember(anotherFirstProjectTeam, currentUser);
        persistTeamMember(secondTeam, currentUser);
        persistTeamMember(noNotificationTeam, currentUser);
        persistTeamMember(firstTeam, otherUser);

        // 유효한 미확인 검토 알림: 첫 프로젝트 2개, 두 번째 프로젝트 1개
        QaQuestion firstQuestion = persistAnsweredQuestion(
                currentUser,
                firstTeam,
                "첫 번째 질문"
        );
        QaQuestion secondQuestion = persistAnsweredQuestion(
                currentUser,
                firstTeam,
                "두 번째 질문"
        );
        QaQuestion thirdQuestion = persistAnsweredQuestion(
                currentUser,
                secondTeam,
                "세 번째 질문"
        );
        persistReviewNotification(currentUser, firstProject, firstTeam, firstQuestion.getQuestionId());
        persistReviewNotification(currentUser, firstProject, firstTeam, secondQuestion.getQuestionId());
        persistReviewNotification(currentUser, secondProject, secondTeam, thirdQuestion.getQuestionId());

        QaQuestion readQuestion = persistAnsweredQuestion(
                currentUser,
                firstTeam,
                "이미 읽은 질문"
        );

        Notification readNotification = reviewNotification(
                currentUser,
                firstProject,
                firstTeam,
                readQuestion.getQuestionId()
        );
        readNotification.markAsRead();
        entityManager.persist(readNotification);

        QaQuestion hiddenQuestion = persistAnsweredQuestion(
                currentUser,
                firstTeam,
                "숨긴 질문"
        );
        Notification hiddenNotification = reviewNotification(
                currentUser,
                firstProject,
                firstTeam,
                hiddenQuestion.getQuestionId()
        );
        hiddenNotification.hide();
        entityManager.persist(hiddenNotification);

        // 현재 사용자가 더 이상 소속되지 않은 팀의 과거 알림
        QaQuestion formerTeamQuestion = persistAnsweredQuestion(
                currentUser,
                formerTeam,
                "탈퇴한 팀 질문"
        );
        persistReviewNotification(
                currentUser,
                firstProject,
                formerTeam,
                formerTeamQuestion.getQuestionId()
        );

        // 다른 사용자에게 발송된 알림
        QaQuestion otherUserQuestion = persistAnsweredQuestion(
                otherUser,
                firstTeam,
                "다른 사용자 질문"
        );
        persistReviewNotification(
                otherUser,
                firstProject,
                firstTeam,
                otherUserQuestion.getQuestionId()
        );

        // 질문 관련 알림이지만 검토 요청 타입이 아닌 알림
        QaQuestion answeredNotificationQuestion = persistAnsweredQuestion(
                currentUser,
                firstTeam,
                "일반 답변 알림 질문"
        );
        entityManager.persist(Notification.builder()
                .user(currentUser)
                .type(NotificationType.QA_ANSWERED)
                .referenceType(NotificationReferenceType.QA_QUESTION)
                .referenceId(answeredNotificationQuestion.getQuestionId())
                .projectId(firstProject.getProjectId())
                .teamId(firstTeam.getTeamId())
                .message("답변 등록 알림")
                .build());

        entityManager.flush();
        entityManager.clear();

        List<HomeAiAnswerCountProjection> result =
                homeAiAnswerRepository.findUnreadAiAnswerCountsByProject(
                        currentUser.getUserId(),
                        NotificationType.QA_AI_REVIEW_REQUIRED,
                        NotificationReferenceType.QA_QUESTION
                );

        assertThat(result)
                .extracting(
                        HomeAiAnswerCountProjection::getProjectId,
                        HomeAiAnswerCountProjection::getUnreadCount
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                firstProject.getProjectId(),
                                2L
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                secondProject.getProjectId(),
                                1L
                        )
                );

        assertThat(homeAiAnswerRepository.findProjectsByTeamMemberUserId(
                currentUser.getUserId()
        )).extracting(
                projection -> projection.getProjectName()
        ).containsExactly(
                "첫 번째 프로젝트",
                "두 번째 프로젝트",
                "알림 없는 프로젝트"
        );
    }

    @Test
    void pagesNotificationsFirstAndLoadsTheirCardDetailsInOneBatch() {
        Organization organization = persistOrganization();
        User reviewer = persistUser(
                organization,
                "card-reviewer@noddi.test",
                "카드 검토자"
        );
        Project project = persistProject(
                organization,
                reviewer,
                "카드 프로젝트"
        );
        Team team = persistTeam(project, reviewer, "카드 팀");
        persistProjectMember(project, reviewer);
        persistTeamMember(team, reviewer);

        QaQuestion olderQuestion = persistAnsweredQuestion(
                reviewer,
                team,
                "오래된 질문"
        );
        Notification olderNotification = reviewNotification(
                reviewer,
                project,
                team,
                olderQuestion.getQuestionId()
        );
        entityManager.persist(olderNotification);

        QaQuestion newerQuestion = persistAnsweredQuestion(
                reviewer,
                team,
                "최신 질문"
        );
        QaAnswer newerAnswer = findAnswer(newerQuestion);

        // 저장 순서와 무관하게 citationIndex 오름차순으로 반환되는지 확인한다.
        entityManager.persist(QaAnswerSource.builder()
                .answer(newerAnswer)
                .citationIndex(2)
                .sourceType(SourceType.TEAM_TEXT)
                .referenceId(202L)
                .sourceTitle("랜딩 페이지 문구 공유 페이지")
                .excerpt("최종 문구와 검토 결과를 정리했습니다.")
                .build());
        entityManager.persist(QaAnswerSource.builder()
                .answer(newerAnswer)
                .citationIndex(1)
                .sourceType(SourceType.TRANSCRIPT)
                .referenceId(101L)
                .sourceTitle("6월 5일 마케팅-디자인팀 회의")
                .excerpt("랜딩 페이지 최종 문구를 확정했습니다.")
                .build());
        Notification newerNotification = reviewNotification(
                reviewer,
                project,
                team,
                newerQuestion.getQuestionId()
        );
        entityManager.persist(newerNotification);

        // 실제 질문과 답변이 없는 고아 알림은 카드와 개수에서 모두 제외한다.
        persistReviewNotification(reviewer, project, team, 999999L);

        entityManager.flush();
        entityManager.clear();

        Page<Notification> firstPage =
                homeAiAnswerRepository.findUnreadAiAnswerNotifications(
                        reviewer.getUserId(),
                        project.getProjectId(),
                        NotificationType.QA_AI_REVIEW_REQUIRED,
                        NotificationReferenceType.QA_QUESTION,
                        PageRequest.of(0, 1)
                );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.getContent())
                .extracting(Notification::getReferenceId)
                .containsExactly(newerQuestion.getQuestionId());

        List<QaAnswer> cardDetails =
                homeAiAnswerDetailRepository.findAllCardDetailsByQuestionIds(
                        List.of(
                                olderQuestion.getQuestionId(),
                                newerQuestion.getQuestionId()
                        )
                );

        assertThat(cardDetails)
                .extracting(answer -> answer.getQuestion().getContent())
                .containsExactlyInAnyOrder("오래된 질문", "최신 질문");
        assertThat(cardDetails)
                .allSatisfy(answer -> {
                    assertThat(answer.getQuestion().getQuestioner().getName())
                            .isEqualTo("카드 검토자");
                    assertThat(answer.getQuestion().getTargetTeam().getName())
                            .isEqualTo("카드 팀");
                    assertThat(answer.getQuestion().getTargetTeam()
                            .getProject().getName())
                            .isEqualTo("카드 프로젝트");
                });

        HomeAiAnswerResponseDto.CardPage response =
                homeAiAnswerQueryService.getUnreadAnswerCards(
                        reviewer.getUserId(),
                        project.getProjectId(),
                        0,
                        1
                );

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();

        HomeAiAnswerResponseDto.Card card = response.getItems().getFirst();
        assertThat(card.getNotificationId())
                .isEqualTo(newerNotification.getNotificationId());
        assertThat(card.getQuestionId())
                .isEqualTo(newerQuestion.getQuestionId());
        assertThat(card.getQuestioner().getName()).isEqualTo("카드 검토자");
        assertThat(card.getQuestioner().getDepartment()).isEqualTo("개발팀");
        assertThat(card.getQuestioner().getPosition()).isEqualTo("개발자");
        assertThat(card.getAnswerContent()).isEqualTo("AI 답변: 최신 질문");

        assertThat(card.getSources())
                .extracting(HomeAiAnswerResponseDto.Source::getCitationIndex)
                .containsExactly(1, 2);

        HomeAiAnswerResponseDto.Source meetingSource =
                card.getSources().get(0);
        assertThat(meetingSource.getNavigation().getType())
                .isEqualTo(HomeAiAnswerSourceNavigationType.MEETING_SUMMARY);
        assertThat(meetingSource.getNavigation().getMeetingId())
                .isEqualTo(101L);

        HomeAiAnswerResponseDto.Source teamPageSource =
                card.getSources().get(1);
        assertThat(teamPageSource.getNavigation().getType())
                .isEqualTo(HomeAiAnswerSourceNavigationType.TEAM_PAGE);
        assertThat(teamPageSource.getNavigation().getTeamId())
                .isEqualTo(team.getTeamId());
        assertThat(teamPageSource.getNavigation().getPageId())
                .isEqualTo(202L);
    }

    @Test
    void readingAHomeCardAlsoRemovesItFromHomeAndTheUnreadNotificationGroup() {
        Organization organization = persistOrganization();
        User reviewer = persistUser(
                organization,
                "read-reviewer@noddi.test",
                "읽음 검토자"
        );
        Project project = persistProject(
                organization,
                reviewer,
                "읽음 연동 프로젝트"
        );
        Team team = persistTeam(project, reviewer, "읽음 연동 팀");
        persistProjectMember(project, reviewer);
        persistTeamMember(team, reviewer);

        QaQuestion question = persistAnsweredQuestion(
                reviewer,
                team,
                "읽음 연동 질문"
        );
        Notification notification = reviewNotification(
                reviewer,
                project,
                team,
                question.getQuestionId()
        );
        entityManager.persist(notification);
        entityManager.flush();
        entityManager.clear();

        HomeAiAnswerResponseDto.ProjectStatus beforeProjectStatus =
                homeAiAnswerQueryService
                        .getUnreadAnswerCountsByProject(reviewer.getUserId())
                        .getFirst();
        HomeAiAnswerResponseDto.CardPage beforeCards =
                homeAiAnswerQueryService.getUnreadAnswerCards(
                        reviewer.getUserId(),
                        project.getProjectId(),
                        0,
                        10
                );
        NotificationResponseDto.NotificationList beforeNotifications =
                notificationQueryService.getNotifications(
                        reviewer.getUserId(),
                        NotificationFilter.UNREAD,
                        0,
                        20
                );

        assertThat(beforeProjectStatus.getUnreadAnswerCount()).isEqualTo(1);
        assertThat(beforeCards.getTotalElements()).isEqualTo(1);
        assertThat(beforeNotifications.getItems()).hasSize(1);
        assertThat(beforeNotifications.getItems().getFirst().getCount())
                .isEqualTo(1);

        // 홈의 자세히보기에서도 기존 개별 알림 읽음 API가 호출하는 서비스 메서드를 그대로 사용한다.
        notificationCommandService.markAsRead(
                reviewer.getUserId(),
                notification.getNotificationId()
        );
        entityManager.flush();
        entityManager.clear();

        HomeAiAnswerResponseDto.ProjectStatus afterProjectStatus =
                homeAiAnswerQueryService
                        .getUnreadAnswerCountsByProject(reviewer.getUserId())
                        .getFirst();
        HomeAiAnswerResponseDto.CardPage afterCards =
                homeAiAnswerQueryService.getUnreadAnswerCards(
                        reviewer.getUserId(),
                        project.getProjectId(),
                        0,
                        10
                );
        NotificationResponseDto.NotificationList afterNotifications =
                notificationQueryService.getNotifications(
                        reviewer.getUserId(),
                        NotificationFilter.UNREAD,
                        0,
                        20
                );

        assertThat(afterProjectStatus.getUnreadAnswerCount()).isZero();
        assertThat(afterCards.getItems()).isEmpty();
        assertThat(afterCards.getTotalElements()).isZero();
        assertThat(afterNotifications.getItems()).isEmpty();
        assertThat(afterNotifications.getUnreadCount()).isZero();
    }

    private Organization persistOrganization() {
        Organization organization = Organization.builder()
                .name("노디")
                .emailDomain("noddi.test")
                .build();
        entityManager.persist(organization);
        return organization;
    }

    private User persistUser(
            Organization organization,
            String email,
            String name
    ) {
        User user = User.builder()
                .organization(organization)
                .email(email)
                .name(name)
                .password("encoded-password")
                .department("개발팀")
                .position("개발자")
                .build();
        entityManager.persist(user);
        return user;
    }

    private Project persistProject(
            Organization organization,
            User creator,
            String name
    ) {
        Project project = Project.builder()
                .organization(organization)
                .name(name)
                .description("테스트 프로젝트")
                .createdBy(creator)
                .build();
        entityManager.persist(project);
        return project;
    }

    private Team persistTeam(
            Project project,
            User creator,
            String name
    ) {
        Team team = Team.builder()
                .project(project)
                .name(name)
                .description("테스트 팀")
                .createdBy(creator)
                .build();
        entityManager.persist(team);
        return team;
    }

    private void persistProjectMember(Project project, User user) {
        entityManager.persist(ProjectMember.builder()
                .project(project)
                .user(user)
                .role(ProjectRole.MEMBER)
                .build());
    }

    private QaQuestion persistAnsweredQuestion(
            User questioner,
            Team targetTeam,
            String content
    ) {
        QaQuestion question = QaQuestion.builder()
                .questioner(questioner)
                .targetTeam(targetTeam)
                .content(content)
                .build();
        entityManager.persist(question);
        entityManager.persist(QaAnswer.createAiAnswer(
                question,
                "AI 답변: " + content
        ));
        return question;
    }

    private QaAnswer findAnswer(QaQuestion question) {
        return entityManager.createQuery(
                        "SELECT answer FROM QaAnswer answer "
                                + "WHERE answer.question = :question",
                        QaAnswer.class
                )
                .setParameter("question", question)
                .getSingleResult();
    }

    private void persistTeamMember(Team team, User user) {
        entityManager.persist(TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.MEMBER)
                .build());
    }

    private void persistReviewNotification(
            User recipient,
            Project project,
            Team team,
            Long questionId
    ) {
        entityManager.persist(reviewNotification(
                recipient,
                project,
                team,
                questionId
        ));
    }

    private Notification reviewNotification(
            User recipient,
            Project project,
            Team team,
            Long questionId
    ) {
        return Notification.builder()
                .user(recipient)
                .type(NotificationType.QA_AI_REVIEW_REQUIRED)
                .referenceType(NotificationReferenceType.QA_QUESTION)
                .referenceId(questionId)
                .projectId(project.getProjectId())
                .teamId(team.getTeamId())
                .message("AI 답변 검토 요청")
                .build();
    }
}
