package com._penLearning.Noddi.domain.team.service;

import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.announcement.repository.AnnouncementRepository;
import com._penLearning.Noddi.domain.meeting.repository.MeetingParticipantRepository;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeDeletionService;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 팀 삭제 전에 팀을 참조하는 모든 하위 데이터를 FK 의존 순서대로 정리한다. */
@Service
@RequiredArgsConstructor
public class TeamResourceDeletionService {

    private final KnowledgeDeletionService knowledgeDeletionService;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;
    private final QaAnswerRevisionRepository qaAnswerRevisionRepository;
    private final QaAnswerRepository qaAnswerRepository;
    private final QaQuestionRepository qaQuestionRepository;
    private final ActionItemRepository actionItemRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final MeetingRepository meetingRepository;
    private final AnnouncementRepository announcementRepository;
    private final TeamPageRepository teamPageRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAllOwnedBy(Team team) {
        Long teamId = team.getTeamId();

        // Pinecone 벡터를 지우면서 팀을 참조하는 QaKnowledgeIndex도 함께 정리한다.
        knowledgeDeletionService.deleteTeamKnowledge(teamId);

        // 질문을 지우기 전에 답변의 출처와 답변부터 삭제해야 한다.
        qaAnswerSourceRepository.bulkDeleteByTeam(team);
        qaAnswerRevisionRepository.bulkDeleteByTeam(team);
        qaAnswerRepository.bulkDeleteByTeam(team);
        qaQuestionRepository.bulkDeleteByTeam(team);

        // 회의를 지우기 전에 회의를 참조하는 하위 데이터를 먼저 삭제해야 한다.
        actionItemRepository.bulkDeleteByTeam(team);
        meetingParticipantRepository.bulkDeleteByTeam(team);
        meetingSummaryRepository.bulkDeleteByTeam(team);
        meetingRepository.bulkDeleteByTeam(team);

        announcementRepository.bulkDeleteByTeam(team);
        teamPageRepository.bulkDeleteByTeam(team);
        teamInviteRepository.deleteAllByTeam(team);
        teamMemberRepository.deleteAllByTeam(team);
    }
}
