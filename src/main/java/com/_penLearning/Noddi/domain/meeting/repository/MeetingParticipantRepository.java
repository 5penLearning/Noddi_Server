package com._penLearning.Noddi.domain.meeting.repository;

import com._penLearning.Noddi.domain.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    List<MeetingParticipant> findByMeeting_MeetingId(Long meetingId);
    boolean existsByMeeting_MeetingIdAndUser_UserId(Long meetingId, Long userId);
}
