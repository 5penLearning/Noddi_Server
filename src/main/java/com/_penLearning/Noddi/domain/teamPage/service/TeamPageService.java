package com._penLearning.Noddi.domain.teamPage.service;

import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.code.TeamPageErrorCode;
import com._penLearning.Noddi.domain.teamPage.dto.TeamPageRequestDto;
import com._penLearning.Noddi.domain.teamPage.dto.TeamPageResponseDto;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import com._penLearning.Noddi.domain.teamPage.event.TeamPageChangedEvent;
import com._penLearning.Noddi.domain.teamPage.event.TeamPageDeletedEvent;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamPageService {

    private final TeamPageRepository teamPageRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TeamPageResponseDto.Result createPage(Long teamId, Long authorId, TeamPageRequestDto.Create request){
        Team team = teamRepository.findById(teamId)
                .orElseThrow(()-> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
        User author = userRepository.findById(authorId)
                .orElseThrow(()-> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if(!teamMemberRepository.existsByTeamAndUser(team, author)){
            throw new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        TeamPage teamPage = TeamPage.builder()
                .team(team)
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        TeamPage savedPage = teamPageRepository.save(teamPage);
        eventPublisher.publishEvent(new TeamPageChangedEvent(savedPage.getPageId()));
        return TeamPageResponseDto.Result.from(savedPage);
    }

    @Transactional
    public TeamPageResponseDto.Result updatePage(Long teamId, Long teamPageId, Long requesterId, TeamPageRequestDto.Update request){
        Team team = teamRepository.findById(teamId)
                .orElseThrow(()-> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(()-> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!teamMemberRepository.existsByTeamAndUser(team, requester)) {
            throw new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        TeamPage teamPage = teamPageRepository.findByPageIdAndTeam(teamPageId, team)
                .orElseThrow(() -> new GeneralException(TeamPageErrorCode.TEAM_PAGE_NOT_FOUND));


        if (!teamPage.getAuthor().getUserId().equals(requesterId)) {
            throw new GeneralException(TeamPageErrorCode.YOU_ARE_NOT_AUTHOR);
        }

        teamPage.update(request.getTitle(), request.getContent());
        eventPublisher.publishEvent(new TeamPageChangedEvent(teamPage.getPageId()));

        return TeamPageResponseDto.Result.from(teamPage);
    }

    public Page<TeamPageResponseDto.Summary> getTeamPages(Long teamId, Long userId, Pageable pageable){
        Team team = teamRepository.findById(teamId)
                .orElseThrow(()-> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
        User author = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        if(!teamMemberRepository.existsByTeamAndUser(team, author)){
            throw new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        return teamPageRepository.findAllByTeamWithAuthor(team, pageable)
                .map(TeamPageResponseDto.Summary::from);
    }

    public TeamPageResponseDto.Detail getTeamPage(Long teamId, Long pageId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        if (!teamMemberRepository.existsByTeamAndUser(team, author)) {
            throw new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND);
        }
        TeamPage teamPage = teamPageRepository.findByPageIdAndTeam(pageId, team)
                .orElseThrow(()-> new GeneralException(TeamPageErrorCode.TEAM_PAGE_NOT_FOUND));
        return TeamPageResponseDto.Detail.from(teamPage);
    }

    @Transactional
    public void deleteTeamPage(Long teamId, Long pageId, Long requesterId){
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!teamMemberRepository.existsByTeamAndUser(team, requester)) {
            throw new GeneralException(TeamErrorCode.TEAM_MEMBER_NOT_FOUND);
        }

        TeamPage teamPage = teamPageRepository.findByPageIdAndTeam(pageId, team)
                .orElseThrow(()-> new GeneralException(TeamPageErrorCode.TEAM_PAGE_NOT_FOUND));

        if(!teamPage.getAuthor().getUserId().equals(requesterId)){
           throw new GeneralException(TeamPageErrorCode.YOU_ARE_NOT_AUTHOR);
        }
        teamPageRepository.delete(teamPage);
        eventPublisher.publishEvent(new TeamPageDeletedEvent(teamPage.getPageId()));
    }
}
