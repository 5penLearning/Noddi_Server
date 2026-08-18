package com._penLearning.Noddi.domain.team.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // 특정 팀의 전체 멤버 목록 조회
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE tm.team = :team")
    List<TeamMember> findAllByTeamWithUser(@Param("team") Team team);

    // 특정 유저가 속한 모든 팀 정보 조회 (팀 목록 조회용)
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team WHERE tm.user = :user")
    List<TeamMember> findAllByUserWithTeam(@Param("user") User user);

    // 팀별 개인 To-do 조립을 위해 팀과 프로젝트를 고정된 순서로 일괄 조회한다.
    @Query("""
            SELECT tm
            FROM TeamMember tm
            JOIN FETCH tm.team team
            JOIN FETCH team.project project
            WHERE tm.user = :user
            ORDER BY project.projectId ASC, team.teamId ASC
            """)
    List<TeamMember> findAllByUserWithTeamAndProject(
            @Param("user") User user
    );

    // 특정 유저의 팀 멤버 정보 단건 조회
    Optional<TeamMember> findByTeamAndUser(Team team, User user);

    // 이미 팀에 가입된 유저인지 확인
    boolean existsByTeamAndUser(Team team, User user);

    // 팀 삭제 전 연관된 멤버 일괄 삭제용
    void deleteAllByTeam(Team team);

    // 팀 내 특정 권한(LEADER)을 가진 멤버 수 카운트 (마지막 리더 검증용)
    long countByTeamAndRole(Team team, TeamRole role);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TeamMember tm WHERE tm.team.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);
}
