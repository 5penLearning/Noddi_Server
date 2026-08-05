package com._penLearning.Noddi.domain.team.repository;

import com._penLearning.Noddi.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeam_TeamId(Long teamId);
    List<TeamMember> findByUser_UserId(Long userId);
    Optional<TeamMember> findByTeam_TeamIdAndUser_UserId(Long teamId, Long userId);
    boolean existsByTeam_TeamIdAndUser_UserId(Long teamId, Long userId);
}
