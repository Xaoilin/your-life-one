package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.ReminderEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.ReminderId;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/** Receipts of sent deadline reminders; claiming one twice fails, so a reminder is sent once. */
public interface ReminderRepository extends JpaRepository<ReminderEntity, ReminderId> {
    /** @return true when this call recorded the receipt, false when the reminder was already sent */
    default boolean claim(UUID userId, LocalDate date, String prayer, Instant deadlineAt, Instant sentAt) {
        return insertIfAbsent(userId, date, prayer, deadlineAt, sentAt) == 1;
    }

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prayer.reminder (user_id, prayer_date, prayer_name, deadline_at, sent_at)
            VALUES (:userId, :date, :prayer, :deadlineAt, :sentAt)
            ON CONFLICT (user_id, prayer_date, prayer_name) DO NOTHING""", nativeQuery = true)
    int insertIfAbsent(UUID userId, LocalDate date, String prayer, Instant deadlineAt, Instant sentAt);

    /** Receipts whose deadline is still ahead. */
    List<ReminderEntity> findByUserIdAndDeadlineAtAfter(UUID userId, Instant now);

    /** Locks the receipt until the surrounding transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM ReminderEntity r
            WHERE r.userId = :userId AND r.prayerDate = :date AND r.prayerName = :prayer""")
    Optional<ReminderEntity> findForUpdate(UUID userId, LocalDate date, String prayer);

    /** @return 1 when this call claimed the one re-send after a snooze, 0 when it was already claimed */
    @Modifying
    @Transactional
    @Query("""
            UPDATE ReminderEntity r SET r.resentAt = :now
            WHERE r.userId = :userId AND r.prayerDate = :date AND r.prayerName = :prayer
              AND r.resentAt IS NULL AND r.snoozedUntil <= :now""")
    int claimResend(UUID userId, LocalDate date, String prayer, Instant now);
}
