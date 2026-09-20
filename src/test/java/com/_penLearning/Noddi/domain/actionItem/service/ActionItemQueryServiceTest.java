package com._penLearning.Noddi.domain.actionItem.service;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(ActionItemQueryService.class)
class ActionItemQueryServiceTest {

    @Autowired
    private ActionItemQueryService actionItemQueryService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void groupsMyActionItemsByTeamAndKeepsTeamsWithoutTodos() {
        Organization organization = persistOrganization();
        User currentUser = persistUser(
                organization,
                "todo-user@noddi.test",
                "투두 사용자"
        );
        User otherUser = persistUser(
                organization,
                "other-user@noddi.test",
                "다른 사용자"
        );
        Project project = persistProject(organization, currentUser);

        Team teamWithTodo = persistTeam(project, currentUser, "할 일 있는 팀");
        Team teamWithoutTodo = persistTeam(project, currentUser, "할 일 없는 팀");
        persistTeamMember(teamWithTodo, currentUser);
        persistTeamMember(teamWithoutTodo, currentUser);
        persistTeamMember(teamWithTodo, otherUser);

        Meeting meeting = persistMeeting(teamWithTodo, currentUser);
        persistActionItem(
                meeting,
                currentUser,
                "API 명세 작성",
                LocalDate.of(2026, 8, 20)
        );
        persistCompletedActionItem(
                meeting,
                currentUser,
                "완료된 API 초안 작성",
                LocalDate.of(2026, 8, 18)
        );

        // 같은 팀의 다른 사용자 할 일은 현재 사용자의 목록에 포함되지 않는다.
        persistActionItem(
                meeting,
                otherUser,
                "프론트 화면 구현",
                LocalDate.of(2026, 8, 19)
        );

        entityManager.flush();
        entityManager.clear();

        List<ActionItemResponseDto.TeamTodoGroup> result =
                actionItemQueryService.getMyActionItemsByTeam(
                        currentUser.getUserId()
                );

        assertThat(result)
                .extracting(ActionItemResponseDto.TeamTodoGroup::getTeamName)
                .containsExactly("할 일 있는 팀", "할 일 없는 팀");

        ActionItemResponseDto.TeamTodoGroup firstTeam = result.getFirst();
        assertThat(firstTeam.getProjectId()).isEqualTo(project.getProjectId());
        assertThat(firstTeam.getProjectName()).isEqualTo("투두 프로젝트");
        assertThat(firstTeam.getTodoCount()).isEqualTo(1);
        assertThat(firstTeam.getActionItems())
                .extracting(ActionItemResponseDto.Info::getContent)
                .containsExactly("API 명세 작성");

        ActionItemResponseDto.TeamTodoGroup secondTeam = result.get(1);
        assertThat(secondTeam.getTodoCount()).isZero();
        assertThat(secondTeam.getActionItems()).isEmpty();

        assertThat(actionItemQueryService.getMyActionItems(currentUser.getUserId()))
                .extracting(ActionItemResponseDto.Info::getContent)
                .containsExactly("API 명세 작성");
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
                .build();
        entityManager.persist(user);
        return user;
    }

    private Project persistProject(
            Organization organization,
            User creator
    ) {
        Project project = Project.builder()
                .organization(organization)
                .name("투두 프로젝트")
                .description("팀별 개인 투두 테스트")
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

    private void persistTeamMember(Team team, User user) {
        entityManager.persist(TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamRole.MEMBER)
                .build());
    }

    private Meeting persistMeeting(Team team, User creator) {
        Meeting meeting = Meeting.builder()
                .team(team)
                .title("주간 회의")
                .agenda("개발 일정 확인")
                .createdBy(creator)
                .build();
        entityManager.persist(meeting);
        return meeting;
    }

    private void persistActionItem(
            Meeting meeting,
            User assignee,
            String content,
            LocalDate dueDate
    ) {
        entityManager.persist(ActionItem.builder()
                .meeting(meeting)
                .assignee(assignee)
                .content(content)
                .dueDate(dueDate)
                .isUncertain(false)
                .build());
    }

    private void persistCompletedActionItem(
            Meeting meeting,
            User assignee,
            String content,
            LocalDate dueDate
    ) {
        ActionItem actionItem = ActionItem.builder()
                .meeting(meeting)
                .assignee(assignee)
                .content(content)
                .dueDate(dueDate)
                .isUncertain(false)
                .build();
        actionItem.update(
                content,
                assignee,
                dueDate,
                ActionItemStatus.COMPLETED
        );
        entityManager.persist(actionItem);
    }
}
