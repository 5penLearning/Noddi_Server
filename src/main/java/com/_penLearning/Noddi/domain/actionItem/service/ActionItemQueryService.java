package com._penLearning.Noddi.domain.actionItem.service;

import com._penLearning.Noddi.domain.actionItem.code.ActionItemStatus;
import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionItemQueryService {

    private static final List<ActionItemStatus> ACTIVE_STATUSES = List.of(
            ActionItemStatus.PENDING,
            ActionItemStatus.IN_PROGRESS
    );

    private final ActionItemRepository actionItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public List<ActionItemResponseDto.Info> getMyActionItems(Long currentUserId) {
        if (!userRepository.existsById(currentUserId)) {
            throw new GeneralException(UserErrorCode.USER_NOT_FOUND);
        }

        return actionItemRepository.findAllByAssigneeIdWithDetails(
                        currentUserId,
                        ACTIVE_STATUSES
                )
                .stream()
                .map(ActionItemResponseDto.Info::from)
                .toList();
    }

    /**
     * 현재 사용자의 모든 소속 팀을 기준으로 개인 ActionItem을 그룹화한다.
     *
     * ActionItem부터 그룹화하면 할 일이 없는 팀이 사라지므로 TeamMember 목록을
     * 응답의 기준으로 삼고, 별도로 조회한 ActionItem을 teamId로 연결한다.
     */
    public List<ActionItemResponseDto.TeamTodoGroup>
    getMyActionItemsByTeam(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new GeneralException(UserErrorCode.USER_NOT_FOUND)
                );

        List<TeamMember> teamMemberships =
                teamMemberRepository
                        .findAllByUserWithTeamAndProject(currentUser);

        Map<Long, List<ActionItem>> actionItemsByTeamId =
                actionItemRepository
                        .findAllByAssigneeIdWithTeamDetails(
                                currentUserId,
                                ACTIVE_STATUSES
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                actionItem -> actionItem.getMeeting()
                                        .getTeam()
                                        .getTeamId()
                        ));

        return teamMemberships.stream()
                .map(teamMember -> {
                    Long teamId = teamMember.getTeam().getTeamId();

                    return ActionItemResponseDto.TeamTodoGroup.of(
                            teamMember.getTeam(),
                            actionItemsByTeamId.getOrDefault(
                                    teamId,
                                    List.of()
                            )
                    );
                })
                .toList();
    }
}
