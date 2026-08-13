package com._penLearning.Noddi.domain.summary.repository;

import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;

import java.util.List;
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

    @Query("""
            SELECT ms.meeting.meetingId
            FROM MeetingSummary ms
            WHERE ms.meeting.aiStatus = :aiStatus
              AND NOT EXISTS (
                  SELECT index.indexId
                  FROM QaKnowledgeIndex index
                  WHERE index.sourceId = ms.meeting.meetingId
                    AND index.sourceType = :sourceType
              )
            ORDER BY ms.createdAt ASC
            """)
    List<Long> findUnindexedMeetingIds(
            @Param("aiStatus") AiStatus aiStatus,
            @Param("sourceType") SourceType sourceType,
            Pageable pageable
    );
}
