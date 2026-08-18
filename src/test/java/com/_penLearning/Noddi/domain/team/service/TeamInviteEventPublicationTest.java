package com._penLearning.Noddi.domain.team.service;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamInvite;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.team.event.TeamInviteCreatedEvent;
import com._penLearning.Noddi.domain.team.event.TeamInviteRespondedEvent;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamInviteEventPublicationTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamInviteRepository teamInviteRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamInviteExpirationService teamInviteExpirationService;
    @Mock private TeamResourceDeletionService teamResourceDeletionService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Team team;
    @Mock private Project project;
    @Mock private User requester;
    @Mock private User targetUser;
    @Mock private TeamMember leaderMembership;
    @Mock private ProjectMember requesterProjectMembership;
    @Mock private ProjectMember targetProjectMembership;

    @Test
    void publishesTeamInviteCreatedEventAfterSavingInvite() {
        // Given: 요청자는 팀 리더이고 대상 사용자는 프로젝트 멤버지만 아직 팀에는 속하지 않았다.
        TeamCommandService service = service();
        when(teamRepository.findById(2L)).thenReturn(Optional.of(team));
        when(userRepository.findById(20L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(team.getProject()).thenReturn(project);
        when(projectMemberRepository.findByProjectAndUser(project, requester))
                .thenReturn(Optional.of(requesterProjectMembership));
        when(teamMemberRepository.findByTeamAndUser(team, requester))
                .thenReturn(Optional.of(leaderMembership));
        when(leaderMembership.getRole()).thenReturn(TeamRole.LEADER);
        when(projectMemberRepository.findByProjectAndUser(project, targetUser))
                .thenReturn(Optional.of(targetProjectMembership));
        when(teamMemberRepository.existsByTeamAndUser(team, targetUser)).thenReturn(false);
        when(teamInviteRepository.existsByTeamAndInviteeAndStatus(
                any(), any(), any()
        )).thenReturn(false);
        when(project.getProjectId()).thenReturn(1L);
        when(project.getName()).thenReturn("노디프로젝트");
        when(team.getTeamId()).thenReturn(2L);
        when(team.getName()).thenReturn("마케팅팀");
        when(requester.getName()).thenReturn("홍길동");
        when(targetUser.getUserId()).thenReturn(10L);

        // 저장소가 IDENTITY PK를 부여하는 실제 동작을 테스트에서 재현한다.
        doAnswer(invocation -> {
            TeamInvite savedInvite = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedInvite, "inviteId", 300L);
            return savedInvite;
        }).when(teamInviteRepository).save(any(TeamInvite.class));

        // When: 팀 초대를 생성한다.
        service.inviteTeamMember(2L, 20L, 10L);

        // Then: 저장된 초대 ID와 표시 정보를 담은 이벤트를 발행한다.
        ArgumentCaptor<TeamInviteCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(TeamInviteCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new TeamInviteCreatedEvent(
                300L,
                1L,
                "노디프로젝트",
                2L,
                "마케팅팀",
                "홍길동",
                10L
        ));
    }

    @Test
    void publishesTeamInviteRespondedEventAfterAcceptingInvite() {
        // Given: 현재 사용자가 받은 유효한 팀 초대를 수락한다.
        TeamCommandService service = service();
        TeamInvite invite = TeamInvite.builder()
                .team(team)
                .inviter(requester)
                .invitee(targetUser)
                .build();
        ReflectionTestUtils.setField(invite, "inviteId", 300L);
        ReflectionTestUtils.setField(
                invite,
                "createdAt",
                LocalDateTime.now()
        );

        when(teamInviteRepository.findByIdWithTeam(300L))
                .thenReturn(Optional.of(invite));
        when(targetUser.getUserId()).thenReturn(10L);
        when(team.getProject()).thenReturn(project);
        when(projectMemberRepository.findByProjectAndUser(project, targetUser))
                .thenReturn(Optional.of(targetProjectMembership));
        when(teamMemberRepository.existsByTeamAndUser(team, targetUser))
                .thenReturn(false);

        // When: 초대 수락과 팀 멤버 저장이 정상적으로 완료된다.
        service.respondToInvite(300L, 10L, true);

        // Then: 커밋 후 초대 알림을 숨길 수 있도록 응답 이벤트를 발행한다.
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(eventPublisher).publishEvent(
                new TeamInviteRespondedEvent(300L)
        );
    }

    private TeamCommandService service() {
        return new TeamCommandService(
                teamRepository,
                teamMemberRepository,
                teamInviteRepository,
                projectRepository,
                projectMemberRepository,
                userRepository,
                teamInviteExpirationService,
                teamResourceDeletionService,
                eventPublisher
        );
    }
}
