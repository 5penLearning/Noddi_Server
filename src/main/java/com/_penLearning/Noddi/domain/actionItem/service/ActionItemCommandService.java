package com._penLearning.Noddi.domain.actionItem.service;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemErrorCode;
import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemRequestDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActionItemCommandService {
    private final ActionItemRepository actionItemRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

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

        return actionItemRepository.save(actionItem).getActionItemId();
    }
    @Transactional
    public void updateActionItem(Long actionItemId, Long currentUserId, ActionItemRequestDto.Update request) {
        ActionItem actionItem = getActionItemOrThrow(actionItemId);
        User currentUser = getUserOrThrow(currentUserId);
        Meeting meeting = actionItem.getMeeting();
        validateTeamMember(meeting.getTeam(), currentUser);

        User assignee = resolveAssignee(request.getAssigneeUserId(), meeting.getTeam());

        actionItem.update(request.getContent(), assignee, request.getDueDate(), request.getStatus());
    }

    @Transactional
    public void deleteActionItem(Long actionItemId, Long currentUserId) {
        ActionItem actionItem = getActionItemOrThrow(actionItemId);
        User currentUser = getUserOrThrow(currentUserId);
        Meeting meeting = actionItem.getMeeting();
        validateTeamMember(meeting.getTeam(), currentUser);

        actionItemRepository.delete(actionItem);
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
}
