package com._penLearning.Noddi.domain.meeting.repository;

import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.entity.MeetingParticipant;
import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    // 회의 내 특정 참가자 조회 (퇴장 처리 시 사용) - ID조회보다 에러응답 세분화에 용이해서 채택
    Optional<MeetingParticipant> findByMeetingAndUser(Meeting meeting, User user);
    // 특정 회의의 전체 참가자 목록 조회
    List<MeetingParticipant> findAllByMeeting(Meeting meeting);
}
