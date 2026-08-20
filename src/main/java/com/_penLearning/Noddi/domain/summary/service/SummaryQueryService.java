package com._penLearning.Noddi.domain.summary.service;

import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.domain.summary.dto.SummaryResponseDto;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.formatter.TranscriptFormatter;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//조회 전용 서비스로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SummaryQueryService {
    private final MeetingRepository meetingRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TranscriptFormatter transcriptFormatter;

    public SummaryResponseDto.Detail getSummary(Long meetingId, Long currentUserId) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(meeting, user);

        //AI 처리가 완료되지 않는 경우는 조회하지 않고 처리 상태와 빈 결과만 반환
        if (meeting.getAiStatus() != AiStatus.COMPLETED) {
            return SummaryResponseDto.Detail.withoutResult(meeting);
        }

        //meeting을 이미 조회해왔기때문에 findByMeeting_MeetingId 메서드로 조회
        MeetingSummary meetingSummary = meetingSummaryRepository.findByMeeting_MeetingId(meetingId)
                .orElseThrow(() -> new GeneralException(SummaryErrorCode.SUMMARY_NOT_FOUND));
        List<ActionItem> actionItems = actionItemRepository.findAllByMeetingIdWithDetails(meetingId);

        boolean hasSegments =
                meetingSummary.getTranscriptSegments() != null
                        && !meetingSummary.getTranscriptSegments().isEmpty();

        String fallbackTranscript =
                hasSegments
                        ? null
                        : transcriptFormatter.format(
                        meetingSummary.getRawTranscript()
                );

        return SummaryResponseDto.Detail.completed(
                meetingSummary,
                actionItems,
                fallbackTranscript
        );
    }

    private Meeting getMeetingOrThrow(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() ->
                        new GeneralException(
                                MeetingErrorCode.MEETING_NOT_FOUND
                        )
                );
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new GeneralException(
                                MeetingErrorCode.USER_NOT_FOUND
                        )
                );
    }

    private void validateTeamMember(
            Meeting meeting,
            User user
    ) {
        boolean isTeamMember =
                teamMemberRepository.existsByTeamAndUser(
                        meeting.getTeam(),
                        user
                );

        if (!isTeamMember) {
            throw new GeneralException(
                    MeetingErrorCode.NOT_TEAM_MEMBER
            );
        }
    }
}
