package com._penLearning.Noddi.domain.project.repository;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.project.entity.InviteStatus;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectInvite;
import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, Long> {

    // 1. 중복 초대 방어
    boolean existsByProjectAndInviteeAndStatus(Project project, User invitee, InviteStatus status);

    // 2. 초대장 단건 조회 (N+1 방지)
    @Query("SELECT pi FROM ProjectInvite pi JOIN FETCH pi.project JOIN FETCH pi.invitee WHERE pi.inviteId = :inviteId")
    Optional<ProjectInvite> findByIdWithProject(@Param("inviteId") Long inviteId);

    // 3. 내 초대장 목록 조회 (N+1 방지)
    @Query("SELECT pi FROM ProjectInvite pi JOIN FETCH pi.project JOIN FETCH pi.inviter WHERE pi.invitee = :invitee AND pi.status = :status")
    List<ProjectInvite> findByInviteeAndStatusWithProject(@Param("invitee") User invitee, @Param("status") InviteStatus status);

    // 같은 조직원 중 이미 가입했거나 대기 중인 초대가 있는 사용자를 DB에서 제외한다.
    @Query(
            value = """
                    SELECT u
                    FROM User u
                    WHERE u.organization = :organization
                      AND NOT EXISTS (
                          SELECT pm.projectMemberId
                          FROM ProjectMember pm
                          WHERE pm.project = :project
                            AND pm.user = u
                      )
                      AND NOT EXISTS (
                          SELECT pi.inviteId
                          FROM ProjectInvite pi
                          WHERE pi.project = :project
                            AND pi.invitee = u
                            AND pi.status = :pendingStatus
                      )
                      AND (
                          :keyword IS NULL
                          OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    ORDER BY u.name ASC, u.userId ASC
                    """,
            countQuery = """
                    SELECT COUNT(u)
                    FROM User u
                    WHERE u.organization = :organization
                      AND NOT EXISTS (
                          SELECT pm.projectMemberId
                          FROM ProjectMember pm
                          WHERE pm.project = :project
                            AND pm.user = u
                      )
                      AND NOT EXISTS (
                          SELECT pi.inviteId
                          FROM ProjectInvite pi
                          WHERE pi.project = :project
                            AND pi.invitee = u
                            AND pi.status = :pendingStatus
                      )
                      AND (
                          :keyword IS NULL
                          OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<User> findInvitableUsers(
            @Param("organization") Organization organization,
            @Param("project") Project project,
            @Param("pendingStatus") InviteStatus pendingStatus,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 4. 프로젝트 삭제 시 연관 초대장 일괄 삭제 (벌크 연산)
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProjectInvite p WHERE p.project = :project")
    void deleteBulkByProject(@Param("project") Project project);

    // 5. 유효기간 만료 초대장 일괄 처리 (벌크 연산)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectInvite p SET p.status = :expiredStatus, p.version = p.version + 1 WHERE p.status = :pendingStatus AND p.createdAt < :expirationThreshold")
    int bulkExpireInvitations(
            @Param("expirationThreshold") LocalDateTime expirationThreshold,
            @Param("pendingStatus") InviteStatus pendingStatus,
            @Param("expiredStatus") InviteStatus expiredStatus
    );
}
