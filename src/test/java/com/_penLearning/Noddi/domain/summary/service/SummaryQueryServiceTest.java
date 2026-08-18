package com._penLearning.Noddi.domain.summary.service;

import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.formatter.TranscriptFormatter;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryQueryServiceTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingSummaryRepository meetingSummaryRepository;
    @Mock private ActionItemRepository actionItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @Mock private Meeting meeting;
    @Mock private MeetingSummary meetingSummary;
    @Mock private User user;
    @Mock private Team team;

    @Test
    void returnsSentenceSeparatedTranscriptWithoutChangingStoredValue() {
        // Given: Whisper 원문은 DB에 한 줄로 저장되어 있다.
        String storedTranscript =
                "첫 번째 안건을 논의했습니다. 상준님이 금요일까지 수정안을 작성해주세요.";

        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(meeting));
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));
        when(meeting.getTeam()).thenReturn(team);
        when(teamMemberRepository.existsByTeamAndUser(team, user))
                .thenReturn(true);
        when(meeting.getAiStatus()).thenReturn(AiStatus.COMPLETED);
        when(meetingSummaryRepository.findByMeeting_MeetingId(1L))
                .thenReturn(Optional.of(meetingSummary));
        when(actionItemRepository.findAllByMeetingIdWithDetails(1L))
                .thenReturn(List.of());
        when(meetingSummary.getMeeting()).thenReturn(meeting);
        when(meetingSummary.getRawTranscript()).thenReturn(storedTranscript);

        SummaryQueryService service = new SummaryQueryService(
                meetingRepository,
                meetingSummaryRepository,
                actionItemRepository,
                userRepository,
                teamMemberRepository,
                new TranscriptFormatter()
        );

        // When: 완료된 AI 회의록을 조회한다.
        var response = service.getSummary(1L, 10L);

        // Then: 응답만 문장 단위로 나뉘며 엔티티의 원문 값은 수정하지 않는다.
        assertThat(response.getRawTranscript()).isEqualTo("""
                첫 번째 안건을 논의했습니다.
                상준님이 금요일까지 수정안을 작성해주세요.""");
        assertThat(meetingSummary.getRawTranscript())
                .isEqualTo(storedTranscript);
    }
}
