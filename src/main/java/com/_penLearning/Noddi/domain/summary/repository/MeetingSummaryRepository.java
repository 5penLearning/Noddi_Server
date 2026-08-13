package com._penLearning.Noddi.domain.summary.repository;

import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {
    Optional<MeetingSummary> findByMeeting_MeetingId(Long meetingId);

    @Query("""
            SELECT ms
            FROM MeetingSummary ms
            JOIN FETCH ms.meeting m
            JOIN FETCH m.team
            WHERE m.meetingId = :meetingId
            """)
    Optional<MeetingSummary> findByMeetingIdWithMeetingAndTeam(@Param("meetingId") Long meetingId);
}
