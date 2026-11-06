package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.NoticeEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.NoticeId;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/** Receipts of sent bounded notices; each is claimed once, so a notice goes out once. */
public interface NoticeRepository extends JpaRepository<NoticeEntity, NoticeId> {
    /**
     * Records the receipt unless it (or a receipt of the same moment under another key) exists.
     *
     * @return 1 if recorded, 0 if it existed
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prayer.notice (user_id, key, kind, prayer_date, prayer_name, pillar, fires_at, expires_at,
                sent_at, snooze_count)
            VALUES (:#{#n.userId}, :#{#n.key}, :#{#n.kind}, :#{#n.prayerDate}, :#{#n.prayerName}, :#{#n.pillar},
                :#{#n.firesAt}, :#{#n.expiresAt}, :#{#n.sentAt}, 0)
            ON CONFLICT DO NOTHING""", nativeQuery = true)
    int insertIfAbsent(NoticeEntity n);

    List<NoticeEntity> findByUserIdAndKeyIn(UUID userId, Collection<String> keys);

    /** Receipts of notices that have not expired yet. */
    List<NoticeEntity> findByUserIdAndExpiresAtAfter(UUID userId, Instant now);

    /** Locks a prayer's opportunity receipts for the date until the surrounding transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n FROM NoticeEntity n
            WHERE n.userId = :userId AND n.kind = :kind AND n.prayerDate = :date AND n.prayerName = :prayer
            ORDER BY n.firesAt DESC""")
    List<NoticeEntity> findPrayerNoticesForUpdate(UUID userId, String kind, LocalDate date, String prayer);

    /** Locks the pillars' receipts of the momentum notice fired at that instant. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n FROM NoticeEntity n
            WHERE n.userId = :userId AND n.kind = :kind AND n.prayerDate = :date AND n.firesAt = :firesAt
              AND n.pillar IN :pillars""")
    List<NoticeEntity> findMomentumNoticesForUpdate(UUID userId, String kind, LocalDate date, Instant firesAt,
            Collection<String> pillars);

    /** @return how many of the receipts this call claimed for their one re-send after a snooze */
    @Modifying
    @Transactional
    @Query("""
            UPDATE NoticeEntity n SET n.resentAt = :now
            WHERE n.userId = :userId AND n.key IN :keys AND n.resentAt IS NULL AND n.snoozedUntil <= :now""")
    int claimResend(UUID userId, Collection<String> keys, Instant now);
}
