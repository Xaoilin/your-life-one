package com.yourlifeone.planner.gamification.infrastructure.persistence.repository;

import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.ProfileEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {
    /** Creates an empty profile unless the user has one; safe when two requests race. */
    @Modifying
    @Query(value = "INSERT INTO gamification.profile (user_id, updated_at) VALUES (:userId, :now) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(UUID userId, Instant now);

    /** The profile, locked until the transaction ends: rewards for one user are applied one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProfileEntity p WHERE p.userId = :userId")
    Optional<ProfileEntity> lockByUserId(UUID userId);
}
