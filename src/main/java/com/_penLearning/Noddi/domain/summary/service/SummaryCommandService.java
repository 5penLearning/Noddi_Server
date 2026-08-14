package com._penLearning.Noddi.domain.summary.service;

import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.domain.summary.dto.SummaryRequestDto;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryCommandService {

    private final MeetingRepository meetingRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public void updateSummary(Long meetingId, Long currentUserId, SummaryRequestDto.Update request) {
        Meeting meeting =
                getMeetingOrThrow(meetingId);

        User currentUser =
                getUserOrThrow(currentUserId);

        validateTeamMember(meeting, currentUser);

        MeetingSummary meetingSummary = meetingSummaryRepository.findByMeeting_MeetingId(meetingId)
                        .orElseThrow(() -> new GeneralException(SummaryErrorCode.SUMMARY_NOT_FOUND));

        meetingSummary.update(
                request.getSummary(),
                request.getDecisions(),
                request.getIssues()
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