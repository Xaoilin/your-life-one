package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.OutcomeEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OutcomeRepository extends JpaRepository<OutcomeEntity, UUID> {
    List<OutcomeEntity> findByUserIdAndPrayerDateBetween(UUID userId, LocalDate from, LocalDate to);

    Optional<OutcomeEntity> findByIdAndUserId(UUID id, UUID userId);

    /** Every outcome whose one-time reward receipt is set, oldest recording first. */
    List<OutcomeEntity> findByUserIdAndRewardedTrueOrderByRecordedAtAscPrayerDateAsc(UUID userId);

    @Transactional
    long deleteByIdAndUserId(UUID id, UUID userId);

    /**
     * Inserts the outcome unless the user already has one for that date and prayer.
     *
     * @return 1 if inserted, 0 if one already existed
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prayer.outcome (id, user_id, prayer_date, prayer_name, status, recorded_at, source, task_id,
                rewarded, deadline_at, timing)
            VALUES (:#{#o.id}, :#{#o.userId}, :#{#o.prayerDate}, :#{#o.prayerName.label()}, :#{#o.status.value()},
                :#{#o.recordedAt}, :#{#o.source}, :#{#o.taskId}, :#{#o.rewarded}, :#{#o.deadlineAt},
                :#{#o.timing == null ? null : #o.timing.value()})
            ON CONFLICT ON CONSTRAINT outcome_one_per_prayer DO NOTHING""", nativeQuery = true)
    int insertIfAbsent(OutcomeEntity o);
}
