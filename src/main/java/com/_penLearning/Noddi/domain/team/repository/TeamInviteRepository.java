package com._penLearning.Noddi.domain.team.repository;

import com._penLearning.Noddi.domain.team.entity.InviteStatus;
import com._penLearning.Noddi.domain.team.entity.TeamInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    List<TeamInvite> findByInvitee_UserIdAndStatus(Long userId, InviteStatus status);
    boolean existsByTeam_TeamIdAndInvitee_UserIdAndStatus(Long teamId, Long inviteeId, InviteStatus status);
}
