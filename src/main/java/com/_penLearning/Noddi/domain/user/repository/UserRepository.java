package com._penLearning.Noddi.domain.user.repository;

import com._penLearning.Noddi.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 유저 정보와 소속 조직 정보를 한 번의 쿼리로 조회 (N+1 방어)
    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.userId = :userId")
    Optional<User> findByIdWithOrganization(@Param("userId") Long userId);

    /** 같은 조직 사용자가 입력한 부서를 사용 빈도순으로 중복 없이 조회한다. */
    @Query("""
            SELECT user.department
            FROM User user
            WHERE user.organization.organizationId = :organizationId
              AND user.department IS NOT NULL
              AND user.department <> ''
            GROUP BY user.department
            ORDER BY COUNT(user) DESC, user.department ASC
            """)
    List<String> findPopularDepartments(
            @Param("organizationId") Long organizationId,
            Pageable pageable
    );

    /** 같은 조직 사용자가 입력한 직함을 사용 빈도순으로 중복 없이 조회한다. */
    @Query("""
            SELECT user.position
            FROM User user
            WHERE user.organization.organizationId = :organizationId
              AND user.position IS NOT NULL
              AND user.position <> ''
            GROUP BY user.position
            ORDER BY COUNT(user) DESC, user.position ASC
            """)
    List<String> findPopularPositions(
            @Param("organizationId") Long organizationId,
            Pageable pageable
    );

    /*
     * 동일 사용자의 프로필 이미지 변경과 삭제를 직렬화한다.
     *
     * 두 요청이 동시에 같은 이전 이미지 키를 읽으면
     * 한쪽 새 이미지가 S3에 남을 수 있으므로 쓰기 락을 사용한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user
            FROM User user
            WHERE user.userId = :userId
            """)
    Optional<User> findByIdForProfileImageUpdate(
            @Param("userId") Long userId
    );
}
