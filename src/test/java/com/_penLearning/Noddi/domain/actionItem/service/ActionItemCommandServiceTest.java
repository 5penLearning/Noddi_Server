package com._penLearning.Noddi.domain.actionItem.service;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemRequestDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.actionItem.event.ActionItemAssigneeChangedEvent;
import com._penLearning.Noddi.domain.actionItem.event.ActionItemDeletedEvent;
import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionItemCommandServiceTest {

    @Mock private ActionItemRepository actionItemRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private Meeting meeting;
    @Mock private Team team;
    @Mock private Project project;
    @Mock private User actor;
    @Mock private User oldAssignee;
    @Mock private User newAssignee;

    @Test
    void publishesAssignmentChangedEventAfterCreatingAssignedActionItem() {
        // Given: 팀원이 다른 팀원에게 담당자가 지정된 Action Item을 생성한다.
        ActionItemRequestDto.Create request = createRequest(2L);
        stubActorAndMeeting();
        when(userRepository.findById(2L)).thenReturn(Optional.of(newAssignee));
        when(teamMemberRepository.existsByTeamAndUser(team, newAssignee))
                .thenReturn(true);
        when(actor.getUserId()).thenReturn(1L);
        when(newAssignee.getUserId()).thenReturn(2L);
        stubProjectAndTeamDisplayData();

        // 실제 JPA IDENTITY 저장처럼 저장 시 actionItemId를 부여한다.
        doAnswer(invocation -> {
            ActionItem saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "actionItemId", 500L);
            return saved;
        }).when(actionItemRepository).save(any(ActionItem.class));

        // When: Action Item을 생성한다.
        Long actionItemId = service().createActionItem(100L, 1L, request);

        // Then: 저장된 ID와 새 담당자, 요청자 정보를 포함한 이벤트를 발행한다.
        assertThat(actionItemId).isEqualTo(500L);
        ArgumentCaptor<ActionItemAssigneeChangedEvent> captor =
                ArgumentCaptor.forClass(ActionItemAssigneeChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
                new ActionItemAssigneeChangedEvent(
                        500L,
                        10L,
                        "노디프로젝트",
                        20L,
                        "마케팅팀",
                        2L,
                        1L
                )
        );
    }

    @Test
    void doesNotPublishAssignmentEventWhenCreatedWithoutAssignee() {
        // Given: 담당자를 지정하지 않고 Action Item을 생성한다.
        ActionItemRequestDto.Create request = createRequest(null);
        stubActorAndMeeting();
        doAnswer(invocation -> {
            ActionItem saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "actionItemId", 500L);
            return saved;
        }).when(actionItemRepository).save(any(ActionItem.class));

        // When: 담당자 없는 Action Item을 저장한다.
        service().createActionItem(100L, 1L, request);

        // Then: 알림 수신자가 없으므로 담당자 변경 이벤트를 발행하지 않는다.
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void publishesEventOnlyWhenAssigneeActuallyChanges() {
        // Given: 기존 담당자 2에서 새 담당자 3으로 재배정한다.
        ActionItem actionItem = actionItem(oldAssignee);
        ActionItemRequestDto.Update request = updateRequest(3L);
        stubUpdateLookup(actionItem);
        when(oldAssignee.getUserId()).thenReturn(2L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(newAssignee));
        when(teamMemberRepository.existsByTeamAndUser(team, newAssignee))
                .thenReturn(true);
        when(actor.getUserId()).thenReturn(1L);
        when(newAssignee.getUserId()).thenReturn(3L);
        stubProjectAndTeamDisplayData();

        // When: 담당자를 변경한다.
        service().updateActionItem(500L, 1L, request);

        // Then: 새 담당자 정보로 변경 이벤트를 발행한다.
        ArgumentCaptor<ActionItemAssigneeChangedEvent> captor =
                ArgumentCaptor.forClass(ActionItemAssigneeChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().assigneeId()).isEqualTo(3L);
        assertThat(captor.getValue().actorId()).isEqualTo(1L);
    }

    @Test
    void doesNotPublishEventWhenAssigneeRemainsSame() {
        // Given: 내용과 마감일은 수정하지만 담당자 2는 그대로 유지한다.
        ActionItem actionItem = actionItem(oldAssignee);
        ActionItemRequestDto.Update request = updateRequest(2L);
        stubUpdateLookup(actionItem);
        when(oldAssignee.getUserId()).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(oldAssignee));
        when(teamMemberRepository.existsByTeamAndUser(team, oldAssignee))
                .thenReturn(true);

        // When: 담당자가 같은 수정 요청을 처리한다.
        service().updateActionItem(500L, 1L, request);

        // Then: 중복 배정 알림이 생기지 않도록 이벤트를 발행하지 않는다.
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void publishesDeletedEventAfterDeletingActionItem() {
        // Given: 기존 Action Item이 있고 요청자는 해당 팀의 구성원이다.
        ActionItem actionItem = actionItem(oldAssignee);
        stubUpdateLookup(actionItem);

        // When: Action Item을 삭제한다.
        service().deleteActionItem(500L, 1L);

        // Then: 삭제 커밋 후 기존 배정 알림을 숨길 수 있도록 삭제 이벤트를 발행한다.
        verify(actionItemRepository).delete(actionItem);
        verify(eventPublisher).publishEvent(
                new ActionItemDeletedEvent(500L)
        );
    }

    private ActionItemCommandService service() {
        return new ActionItemCommandService(
                actionItemRepository,
                meetingRepository,
                userRepository,
                teamMemberRepository,
                eventPublisher
        );
    }

    private void stubActorAndMeeting() {
        when(meetingRepository.findByMeetingId(100L))
                .thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(meeting.getTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, actor))
                .thenReturn(true);
    }

    private void stubUpdateLookup(ActionItem actionItem) {
        when(actionItemRepository.findById(500L))
                .thenReturn(Optional.of(actionItem));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(meeting.getTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, actor))
                .thenReturn(true);
    }

    private void stubProjectAndTeamDisplayData() {
        when(team.getProject()).thenReturn(project);
        when(project.getProjectId()).thenReturn(10L);
        when(project.getName()).thenReturn("노디프로젝트");
        when(team.getTeamId()).thenReturn(20L);
        when(team.getName()).thenReturn("마케팅팀");
    }

    private ActionItem actionItem(User assignee) {
        ActionItem actionItem = ActionItem.builder()
                .meeting(meeting)
                .assignee(assignee)
                .content("기존 할 일")
                .isUncertain(false)
                .dueDate(LocalDate.of(2026, 8, 30))
                .build();
        ReflectionTestUtils.setField(
                actionItem,
                "actionItemId",
                500L
        );
        return actionItem;
    }

    private ActionItemRequestDto.Create createRequest(Long assigneeId) {
        ActionItemRequestDto.Create request =
                new ActionItemRequestDto.Create();
        ReflectionTestUtils.setField(request, "content", "새 할 일");
        ReflectionTestUtils.setField(request, "assigneeUserId", assigneeId);
        ReflectionTestUtils.setField(
                request,
                "dueDate",
                LocalDate.of(2026, 9, 1)
        );
        return request;
    }

    private ActionItemRequestDto.Update updateRequest(Long assigneeId) {
        ActionItemRequestDto.Update request =
                new ActionItemRequestDto.Update();
        ReflectionTestUtils.setField(request, "content", "수정된 할 일");
        ReflectionTestUtils.setField(request, "assigneeUserId", assigneeId);
        ReflectionTestUtils.setField(
                request,
                "dueDate",
                LocalDate.of(2026, 9, 2)
        );
        ReflectionTestUtils.setField(
                request,
                "status",
                ActionItemStatus.PENDING
        );
        return request;
    }
}
