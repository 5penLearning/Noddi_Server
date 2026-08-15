package com._penLearning.Noddi.domain.teamPage.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamPageRepository extends JpaRepository<TeamPage, Long> {

    @Query(
            value = """
            SELECT page
            FROM TeamPage page
            JOIN FETCH page.author
            WHERE page.team = :team
            """,
            countQuery = """
            SELECT COUNT(page)
            FROM TeamPage page
            WHERE page.team = :team
            """
    )
    Page<TeamPage> findAllByTeamWithAuthor(
            @Param("team") Team team,
            Pageable pageable
    );

    Optional<TeamPage> findByPageIdAndTeam(Long pageId, Team team);

    @Query("""
            SELECT page
            FROM TeamPage page
            JOIN FETCH page.team
            WHERE page.pageId = :pageId
            """)
    Optional<TeamPage> findByPageIdWithTeam(@Param("pageId") Long pageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TeamPage page WHERE page.team = :team")
    void bulkDeleteByTeam(@Param("team") Team team);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM TeamPage page
            WHERE page.team IN (
                SELECT team
                FROM Team team
                WHERE team.project = :project
            )
            """)
    void bulkDeleteByProject(@Param("project") Project project);
}
