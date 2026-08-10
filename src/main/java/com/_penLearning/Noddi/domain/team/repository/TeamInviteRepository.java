package com._penLearning.Noddi.domain.team.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.InviteStatus;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamInvite;
import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {

    // 이미 '대기 중(PENDING)'인 초대장이 있는지 확인 (중복 초대 발송 방어 로직용)
    boolean existsByTeamAndInviteeAndStatus(Team team, User invitee, InviteStatus status);

    // 특정 유저가 받은 특정 상태(PENDING)의 초대장 목록 조회
    @Query("""
    select ti
    from TeamInvite ti
    join fetch ti.team
    join fetch ti.inviter
    where ti.invitee = :invitee
      and ti.status = :status
    """)    List<TeamInvite> findByInviteeAndStatusWithTeam(@Param("invitee") User invitee, @Param("status") InviteStatus status);

    // 초대장 응답 시 초대장 단건 조회 (초대한 팀 정보까지 한 번에 로드)
    @Query("SELECT ti FROM TeamInvite ti JOIN FETCH ti.team WHERE ti.inviteId = :inviteId")
    Optional<TeamInvite> findByIdWithTeam(@Param("inviteId") Long inviteId);

    // 초대장 일괄 만료 쿼리
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
    update TeamInvite ti
       set ti.status = :expiredStatus
     where ti.status = :pendingStatus
       and ti.createdAt < :expirationThreshold
    """)
    int bulkExpireInvitations(
            @Param("expirationThreshold") LocalDateTime expirationThreshold,
            @Param("pendingStatus") InviteStatus pendingStatus,
            @Param("expiredStatus") InviteStatus expiredStatus
    );

    // 팀 삭제 전 연관된 초대장 일괄 삭제용
    void deleteAllByTeam(Team team);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TeamInvite ti WHERE ti.team.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);
}
