package com._penLearning.Noddi.domain.user.repository;

import com._penLearning.Noddi.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // 유저 정보와 소속 조직 정보를 한 번의 쿼리로 조회 (N+1 방어)
    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.userId = :userId")
    Optional<User> findByIdWithOrganization(@Param("userId") Long userId);
}
