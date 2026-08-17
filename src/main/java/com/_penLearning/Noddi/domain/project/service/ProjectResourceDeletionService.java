package com._penLearning.Noddi.domain.project.service;

import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.announcement.repository.AnnouncementRepository;
import com._penLearning.Noddi.domain.meeting.repository.MeetingParticipantRepository;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectInviteRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.rag.indexing.KnowledgeDeletionService;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 프로젝트 삭제 전에 프로젝트가 소유한 모든 하위 데이터를 FK 의존 순서대로 정리한다. */
@Service
@RequiredArgsConstructor
public class ProjectResourceDeletionService {

    private final KnowledgeDeletionService knowledgeDeletionService;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;
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
    private final TeamRepository teamRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAllOwnedBy(Project project) {
        // Pinecone 벡터와 MySQL의 QaKnowledgeIndex를 팀 삭제 전에 함께 정리한다.
        knowledgeDeletionService.deleteProjectKnowledge(project.getProjectId());

        // Q&A 질문을 참조하는 답변 출처와 답변부터 삭제한다.
        qaAnswerSourceRepository.bulkDeleteByProject(project);
        qaAnswerRepository.bulkDeleteByProject(project);
        qaQuestionRepository.bulkDeleteByProject(project);

        // 회의를 참조하는 하위 데이터를 먼저 삭제한 뒤 회의를 삭제한다.
        actionItemRepository.bulkDeleteByProject(project);
        meetingParticipantRepository.bulkDeleteByProject(project);
        meetingSummaryRepository.bulkDeleteByProject(project);
        meetingRepository.bulkDeleteByProject(project);

        // 팀 소유 자료와 팀 관계 데이터를 지운 뒤 팀 자체를 삭제한다.
        announcementRepository.bulkDeleteByProject(project);
        teamPageRepository.bulkDeleteByProject(project);
        teamInviteRepository.bulkDeleteByProject(project);
        teamMemberRepository.bulkDeleteByProject(project);
        teamRepository.bulkDeleteByProject(project);

        // 마지막으로 프로젝트를 직접 참조하는 초대와 멤버 관계를 정리한다.
        projectInviteRepository.deleteBulkByProject(project);
        projectMemberRepository.bulkDeleteByProject(project);
    }
}
