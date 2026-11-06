package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.PrayerProfileEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PrayerProfileRepository extends JpaRepository<PrayerProfileEntity, UUID> {
    /** Creates the profile unless it exists; safe when two first requests race. */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prayer.profile (user_id, enabled, reminder_enabled, reminder_minutes, tracking_started_at,
                updated_at)
            VALUES (:userId, true, true, :reminderMinutes, :now, :now)
            ON CONFLICT (user_id) DO NOTHING""", nativeQuery = true)
    void insertIfAbsent(UUID userId, int reminderMinutes, Instant now);

    /** Only surviving Auth identities can drive background work, even if a stale token re-enables a profile. */
    @Query(value = """
            SELECT p.user_id FROM prayer.profile p
            WHERE p.enabled AND EXISTS (SELECT 1 FROM auth.users u WHERE u.id = p.user_id)""", nativeQuery = true)
    List<UUID> findEligibleUserIds();

    /** Serializes one dispatch with Auth deletion; the caller must hold the transaction through dispatch. */
    @Query(value = "SELECT id FROM auth.users WHERE id = :userId FOR KEY SHARE", nativeQuery = true)
    Optional<UUID> lockIdentity(UUID userId);

    /** Locks the profile row until the surrounding transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PrayerProfileEntity p WHERE p.userId = :userId")
    Optional<PrayerProfileEntity> findForUpdate(UUID userId);
}
