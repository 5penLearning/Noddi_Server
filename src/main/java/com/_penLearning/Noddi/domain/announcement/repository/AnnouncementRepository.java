package com._penLearning.Noddi.domain.announcement.repository;

import com._penLearning.Noddi.domain.announcement.entity.Announcement;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query(
            value = """
                SELECT announcement
                FROM Announcement announcement
                JOIN FETCH announcement.team team
                WHERE team.project = :project
                """,
            countQuery = """
                SELECT COUNT(announcement)
                FROM Announcement announcement
                WHERE announcement.team.project = :project
                """
    )
    Page<Announcement> findAllByProjectWithTeam(
            @Param("project") Project project,
            Pageable pageable
    );

    @Query("""
        SELECT announcement
        FROM Announcement announcement
        JOIN FETCH announcement.team team
        JOIN FETCH announcement.author
        WHERE announcement.announcementId = :announcementId
          AND team.project = :project
        """)
    Optional<Announcement> findByIdAndProjectWithTeamAndAuthor(
            @Param("announcementId") Long announcementId,
            @Param("project") Project project
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Announcement announcement WHERE announcement.team = :team")
    void bulkDeleteByTeam(@Param("team") Team team);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Announcement announcement WHERE announcement.team.project = :project")
    void bulkDeleteByProject(@Param("project") Project project);
}
