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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamResourceDeletionServiceTest {

    @Mock private KnowledgeDeletionService knowledgeDeletionService;
    @Mock private QaAnswerSourceRepository answerSourceRepository;
    @Mock private QaAnswerRevisionRepository answerRevisionRepository;
    @Mock private QaAnswerRepository answerRepository;
    @Mock private QaQuestionRepository questionRepository;
    @Mock private ActionItemRepository actionItemRepository;
    @Mock private MeetingParticipantRepository meetingParticipantRepository;
    @Mock private MeetingSummaryRepository meetingSummaryRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private TeamPageRepository teamPageRepository;
    @Mock private TeamInviteRepository teamInviteRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private Team team;

    @Test
    void deletesQaResourcesInForeignKeyDependencyOrder() {
        TeamResourceDeletionService service = new TeamResourceDeletionService(
                knowledgeDeletionService,
                answerSourceRepository,
                answerRevisionRepository,
                answerRepository,
                questionRepository,
                actionItemRepository,
                meetingParticipantRepository,
                meetingSummaryRepository,
                meetingRepository,
                announcementRepository,
                teamPageRepository,
                teamInviteRepository,
                teamMemberRepository
        );

        // Given: 수정 이력이 존재하는 팀 전체를 삭제하는 상황이다.
        when(team.getTeamId()).thenReturn(10L);

        // When: 팀이 소유한 모든 하위 데이터를 정리한다.
        service.deleteAllOwnedBy(team);

        /*
         * Then: QaAnswer를 참조하는 Source와 Revision을 먼저 삭제한 후
         * Answer, Question 순서로 삭제해야 FK 제약 위반이 발생하지 않는다.
         *
         * InOrder는 단순 호출 여부뿐 아니라 호출 순서가 바뀌는 회귀까지 잡아낸다.
         */
        InOrder qaDeletionOrder = inOrder(
                answerSourceRepository,
                answerRevisionRepository,
                answerRepository,
                questionRepository
        );

        qaDeletionOrder.verify(answerSourceRepository).bulkDeleteByTeam(team);
        qaDeletionOrder.verify(answerRevisionRepository).bulkDeleteByTeam(team);
        qaDeletionOrder.verify(answerRepository).bulkDeleteByTeam(team);
        qaDeletionOrder.verify(questionRepository).bulkDeleteByTeam(team);
    }
}
