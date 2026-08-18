package com._penLearning.Noddi.domain.project.service;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectInvite;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.project.event.ProjectInviteCreatedEvent;
import com._penLearning.Noddi.domain.project.event.ProjectInviteRespondedEvent;
import com._penLearning.Noddi.domain.project.repository.ProjectInviteRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
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
class ProjectInviteEventPublicationTest {

    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectInviteRepository projectInviteRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectInviteExpirationService projectInviteExpirationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Project project;
    @Mock private Organization organization;
    @Mock private User requester;
    @Mock private User targetUser;
    @Mock private ProjectMember requesterMembership;

    @Test
    void publishesProjectInviteCreatedEventAfterSavingInvite() {
        // Given: 요청자는 프로젝트 리더이고 대상 사용자는 같은 조직의 미가입 사용자다.
        ProjectMemberService service = service();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(20L)).thenReturn(Optional.of(requester));
        when(project.getOrganization()).thenReturn(organization);
        when(targetUser.getOrganization()).thenReturn(organization);
        when(organization.getOrganizationId()).thenReturn(9L);
        when(projectMemberRepository.findByProjectAndUser(project, requester))
                .thenReturn(Optional.of(requesterMembership));
        when(requesterMembership.getRole()).thenReturn(ProjectRole.LEADER);
        when(projectMemberRepository.existsByProjectAndUser(project, targetUser)).thenReturn(false);
        when(projectInviteRepository.existsByProjectAndInviteeAndStatus(
                any(), any(), any()
        )).thenReturn(false);
        when(project.getProjectId()).thenReturn(1L);
        when(project.getName()).thenReturn("노디프로젝트");
        when(requester.getName()).thenReturn("홍길동");
        when(targetUser.getUserId()).thenReturn(10L);

        // 저장소가 IDENTITY PK를 부여하는 실제 동작을 테스트에서 재현한다.
        doAnswer(invocation -> {
            ProjectInvite savedInvite = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedInvite, "inviteId", 400L);
            return savedInvite;
        }).when(projectInviteRepository).save(any(ProjectInvite.class));

        // When: 프로젝트 초대를 생성한다.
        service.inviteUser(1L, 20L, 10L);

        // Then: 저장된 초대 ID와 표시 정보를 담은 이벤트를 발행한다.
        ArgumentCaptor<ProjectInviteCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ProjectInviteCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ProjectInviteCreatedEvent(
                400L,
                1L,
                "노디프로젝트",
                "홍길동",
                10L
        ));
    }

    @Test
    void publishesProjectInviteRespondedEventAfterRejectingInvite() {
        // Given: 현재 사용자가 받은 유효한 프로젝트 초대를 거절한다.
        ProjectMemberService service = service();
        ReflectionTestUtils.setField(service, "inviteValidDays", 7);
        ProjectInvite invite = ProjectInvite.builder()
                .project(project)
                .inviter(requester)
                .invitee(targetUser)
                .build();
        ReflectionTestUtils.setField(invite, "inviteId", 400L);
        ReflectionTestUtils.setField(
                invite,
                "createdAt",
                LocalDateTime.now()
        );

        when(projectInviteRepository.findByIdWithProject(400L))
                .thenReturn(Optional.of(invite));
        when(targetUser.getUserId()).thenReturn(10L);

        // When: 프로젝트 초대를 거절한다.
        service.respondToInvitation(400L, 10L, false);

        // Then: 상태가 REJECTED로 변경되고 알림 정리를 위한 이벤트를 발행한다.
        assertThat(invite.getStatus())
                .isEqualTo(com._penLearning.Noddi.domain.project.entity.InviteStatus.REJECTED);
        verify(eventPublisher).publishEvent(
                new ProjectInviteRespondedEvent(400L)
        );
    }

    private ProjectMemberService service() {
        return new ProjectMemberService(
                projectMemberRepository,
                projectInviteRepository,
                projectRepository,
                userRepository,
                projectInviteExpirationService,
                eventPublisher
        );
    }
}
