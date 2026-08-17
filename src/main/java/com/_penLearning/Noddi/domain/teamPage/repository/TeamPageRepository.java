package com._penLearning.Noddi.domain.teamPage.repository;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * TEAM_TEXT 색인이 없거나 페이지 수정 시각보다 색인 상태가 오래된 페이지를 조회한다.
     * 이벤트 유실 및 일시적인 Pinecone 장애가 발생했을 때 복구 스케줄러가 사용한다.
     */
    @Query("""
            SELECT page.pageId
            FROM TeamPage page
            WHERE NOT EXISTS (
                SELECT knowledgeIndex.indexId
                FROM QaKnowledgeIndex knowledgeIndex
                WHERE knowledgeIndex.sourceId = page.pageId
                  AND knowledgeIndex.sourceType = :sourceType
                  AND knowledgeIndex.updatedAt >= page.updatedAt
            )
            ORDER BY page.updatedAt ASC, page.pageId ASC
            """)
    List<Long> findIdsRequiringKnowledgeSynchronization(
            @Param("sourceType") SourceType sourceType,
            Pageable pageable
    );

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
