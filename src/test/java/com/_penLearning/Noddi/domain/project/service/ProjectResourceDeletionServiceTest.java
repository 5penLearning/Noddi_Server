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
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectResourceDeletionServiceTest {

    @Mock private KnowledgeDeletionService knowledgeDeletionService;
    @Mock private QaAnswerSourceRepository answerSourceRepository;
    @Mock private QaAnswerRepository answerRepository;
    @Mock private QaAnswerRevisionRepository answerRevisionRepository;
    @Mock private QaQuestionRepository questionRepository;
    @Mock private ActionItemRepository actionItemRepository;
    @Mock private MeetingParticipantRepository meetingParticipantRepository;
    @Mock private MeetingSummaryRepository meetingSummaryRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private TeamPageRepository teamPageRepository;
    @Mock private TeamInviteRepository teamInviteRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProjectInviteRepository projectInviteRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private Project project;

    @Test
    void deletesQaResourcesInForeignKeyDependencyOrder() {
        ProjectResourceDeletionService service = new ProjectResourceDeletionService(
                knowledgeDeletionService,
                answerSourceRepository,
                answerRepository,
                answerRevisionRepository,
                questionRepository,
                actionItemRepository,
                meetingParticipantRepository,
                meetingSummaryRepository,
                meetingRepository,
                announcementRepository,
                teamPageRepository,
                teamInviteRepository,
                teamMemberRepository,
                teamRepository,
                projectInviteRepository,
                projectMemberRepository
        );

        // Given: 수정 이력이 포함된 프로젝트 전체를 삭제하는 상황이다.
        when(project.getProjectId()).thenReturn(100L);

        // When: 프로젝트가 소유한 모든 하위 데이터를 정리한다.
        service.deleteAllOwnedBy(project);

        /*
         * Then: 프로젝트 삭제에서도 Source와 Revision을 Answer보다 먼저 삭제하고,
         * 마지막에 Question을 삭제하는 FK 의존 순서를 반드시 지켜야 한다.
         */
        InOrder qaDeletionOrder = inOrder(
                answerSourceRepository,
                answerRevisionRepository,
                answerRepository,
                questionRepository
        );

        qaDeletionOrder.verify(answerSourceRepository).bulkDeleteByProject(project);
        qaDeletionOrder.verify(answerRevisionRepository).bulkDeleteByProject(project);
        qaDeletionOrder.verify(answerRepository).bulkDeleteByProject(project);
        qaDeletionOrder.verify(questionRepository).bulkDeleteByProject(project);
    }
}
