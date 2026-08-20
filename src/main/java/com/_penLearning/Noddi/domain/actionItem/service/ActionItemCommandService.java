package com._penLearning.Noddi.domain.actionItem.service;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemErrorCode;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemRequestDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.actionItem.event.ActionItemAssigneeChangedEvent;
import com._penLearning.Noddi.domain.actionItem.event.ActionItemDeletedEvent;
import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActionItemCommandService {
    private final ActionItemRepository actionItemRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createActionItem(Long meetingId, Long currentUserId, ActionItemRequestDto.Create request) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        User currentUser = getUserOrThrow(currentUserId);
        validateTeamMember(meeting.getTeam(), currentUser);

        User assignee = resolveAssignee(request.getAssigneeUserId(), meeting.getTeam());

        ActionItem actionItem = ActionItem.builder()
                .meeting(meeting)
                .assignee(assignee)
                .content(request.getContent())
                .isUncertain(false)
                .dueDate(request.getDueDate())
                .build();

        ActionItem savedActionItem = actionItemRepository.save(actionItem);

        if(savedActionItem.getAssignee() != null) {
            publishAssigneeChangedEvent(savedActionItem, currentUser.getUserId());
        }

        return savedActionItem.getActionItemId();
    }
    @Transactional
    public void updateActionItem(Long actionItemId, Long currentUserId, ActionItemRequestDto.Update request) {
        ActionItem actionItem = getActionItemOrThrow(actionItemId);

        User currentUser = getUserOrThrow(currentUserId);

        Meeting meeting = actionItem.getMeeting();

        validateTeamMember(meeting.getTeam(), currentUser);

        Long previousAssigneeId = actionItem.getAssignee() != null
                ? actionItem.getAssignee().getUserId()
                : null;

        User newAssignee = resolveAssignee(request.getAssigneeUserId(), meeting.getTeam());

        Long newAssigneeId = newAssignee != null
                ? newAssignee.getUserId()
                : null;

        actionItem.update(request.getContent(), newAssignee, request.getDueDate(), request.getStatus());

        if(!Objects.equals(previousAssigneeId, newAssigneeId)) {
            publishAssigneeChangedEvent(actionItem, currentUser.getUserId());
        }
    }

    @Transactional
    public void deleteActionItem(Long actionItemId, Long currentUserId) {
        ActionItem actionItem = getActionItemOrThrow(actionItemId);

        User currentUser = getUserOrThrow(currentUserId);

        Meeting meeting = actionItem.getMeeting();

        validateTeamMember(meeting.getTeam(), currentUser);

        actionItemRepository.delete(actionItem);

        eventPublisher.publishEvent(new ActionItemDeletedEvent(actionItem.getActionItemId()));
    }

    private User resolveAssignee(Long assigneeUserId, Team team)
    {
        if (assigneeUserId == null) {
            return null;
        }

        User assignee = getUserOrThrow(assigneeUserId);

        if (!teamMemberRepository.existsByTeamAndUser(team, assignee))
        {
            throw new GeneralException(ActionItemErrorCode.ASSIGNEE_NOT_TEAM_MEMBER);
        }
        return assignee;
    }

    private Meeting getMeetingOrThrow(Long meetingId)
    {
        return meetingRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    private ActionItem getActionItemOrThrow(Long actionItemId)
    {
        return actionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new GeneralException(ActionItemErrorCode.ACTION_ITEM_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.USER_NOT_FOUND));
    }

    private void validateTeamMember(Team team, User user) {
        if (!teamMemberRepository.existsByTeamAndUser(team, user))
        {
            throw new GeneralException(
                    MeetingErrorCode.NOT_TEAM_MEMBER
            );
        }
    }

    /**
     * 현재 Action Item의 담당자 상태를 이벤트로 발행한다.
     *
     * 담당자가 해제된 경우 assigneeId는 null이며,
     * 알림 핸들러는 기존 알림만 숨기고 새 알림은 만들지 않는다.
     */
    private void publishAssigneeChangedEvent(
            ActionItem actionItem,
            Long actorId
    ) {
        Team team = actionItem.getMeeting().getTeam();

        Project project = team.getProject();

        User assignee = actionItem.getAssignee();

        eventPublisher.publishEvent(
                new ActionItemAssigneeChangedEvent(
                        actionItem.getActionItemId(),
                        project.getProjectId(),
                        project.getName(),
                        team.getTeamId(),
                        team.getName(),
                        assignee != null
                                ? assignee.getUserId()
                                : null,
                        actorId
                )
        );
    }
}
