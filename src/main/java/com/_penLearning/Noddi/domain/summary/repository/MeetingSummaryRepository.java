package com._penLearning.Noddi.domain.summary.repository;

import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {
    Optional<MeetingSummary> findByMeeting_MeetingId(Long meetingId);
}
