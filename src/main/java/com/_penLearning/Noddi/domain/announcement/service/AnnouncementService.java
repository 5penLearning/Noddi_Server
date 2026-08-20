package com._penLearning.Noddi.domain.announcement.service;


import com._penLearning.Noddi.domain.announcement.code.AnnouncementErrorCode;
import com._penLearning.Noddi.domain.announcement.dto.AnnouncementRequestDto;
import com._penLearning.Noddi.domain.announcement.dto.AnnouncementResponseDto;
import com._penLearning.Noddi.domain.announcement.entity.Announcement;
import com._penLearning.Noddi.domain.announcement.repository.AnnouncementRepository;
import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort LATEST_ANNOUNCEMENT_SORT = Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("announcementId")
    );

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AnnouncementRepository announcementRepository;

    @Transactional
    public AnnouncementResponseDto.Result createAnnouncement(
            Long projectId,
            Long teamId,
            Long authorId,
            AnnouncementRequestDto.Create request
    ) {
        Project project = getProjectOrThrow(projectId);
        Team team = getTeamOrThrow(teamId);
        User author = getUserOrThrow(authorId);

        validateProjectMember(project, author);
        validateTeamBelongsToProject(project, team);
        validateTeamMember(team, author);

        Announcement announcement = Announcement.builder()
                .team(team)
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        Announcement savedAnnouncement = announcementRepository.save(announcement);

        return AnnouncementResponseDto.Result.from(savedAnnouncement);
    }

    @Transactional
    public AnnouncementResponseDto.Result updateAnnouncement(
            Long projectId,
            Long announcementId,
            Long requesterId,
            AnnouncementRequestDto.Update request
    ) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        validateProjectMember(project, requester);

        Announcement announcement = getAnnouncementOrThrow(announcementId, project);
        validateTeamMember(announcement.getTeam(), requester);
        validateAuthor(announcement, requesterId);

        announcement.update(request.getTitle(), request.getContent());

        return AnnouncementResponseDto.Result.from(announcement);
    }

    public AnnouncementResponseDto.Detail getAnnouncement(
            Long projectId,
            Long announcementId,
            Long requesterId
    ) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        validateProjectMember(project, requester);

        Announcement announcement = getAnnouncementOrThrow(announcementId, project);
        return AnnouncementResponseDto.Detail.from(announcement);
    }

    public Page<AnnouncementResponseDto.Summary> getAnnouncements(
            Long projectId,
            Long requesterId,
            Pageable pageable
    ) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        validateProjectMember(project, requester);

        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                LATEST_ANNOUNCEMENT_SORT
        );

        return announcementRepository.findAllByProjectWithTeam(project, fixedPageable)
                .map(AnnouncementResponseDto.Summary::from);
    }

    @Transactional
    public void deleteAnnouncement(Long projectId, Long announcementId, Long requesterId) {
        Project project = getProjectOrThrow(projectId);
        User requester = getUserOrThrow(requesterId);

        validateProjectMember(project, requester);

        Announcement announcement = getAnnouncementOrThrow(announcementId, project);
        validateTeamMember(announcement.getTeam(), requester);
        validateAuthor(announcement, requesterId);

        announcementRepository.delete(announcement);
    }

    //--검증 헬퍼 메서드--
    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateProjectMember(Project project, User user) {
        if (!projectMemberRepository.existsByProjectAndUser(project, user)) {
            throw new GeneralException(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }
    }

    private void validateTeamBelongsToProject(Project project, Team team) {
        if (!team.getProject().getProjectId().equals(project.getProjectId())) {
            throw new GeneralException(TeamErrorCode.TEAM_NOT_FOUND);
        }
    }

    private void validateTeamMember(Team team, User user) {
        if (!teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND);
        }
    }

    private Announcement getAnnouncementOrThrow(Long announcementId, Project project) {
        return announcementRepository.findByIdAndProjectWithTeamAndAuthor(announcementId, project)
                .orElseThrow(() -> new GeneralException(AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND));
    }

    private void validateAuthor(Announcement announcement, Long requesterId) {
        if (!announcement.getAuthor().getUserId().equals(requesterId)) {
            throw new GeneralException(AnnouncementErrorCode.YOU_ARE_NOT_AUTHOR);
        }
    }
}
