package com._penLearning.Noddi.domain.meeting.repository;

import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.team.entity.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByMeetingId(Long meetingId);
    /**
     * 비관적 락(Pessimistic Write Lock) 조회
     * - 사용 시점: start() 호출 시 동시에 여러 명이 눌러도 방이 1개만 생성되도록 보장
     * - 동작: SELECT ... FOR UPDATE → 트랜잭션 종료 전까지 다른 트랜잭션이 대기
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Meeting m WHERE m.meetingId = :meetingId")
    Optional<Meeting> findByIdWithPessimisticLock(@Param("meetingId") Long meetingId);
    List<Meeting> findAllByTeam(Team team);
    Optional<Meeting> findByRoomName(String roomName);
}
