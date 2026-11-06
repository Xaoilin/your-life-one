package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.UserDayEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.UserDayId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface UserDayRepository extends JpaRepository<UserDayEntity, UserDayId> {
    @EntityGraph(attributePaths = "scheduleDay")
    List<UserDayEntity> findByUserIdAndDayBetween(UUID userId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "scheduleDay")
    Optional<UserDayEntity> findFirstByUserIdOrderByDayDesc(UUID userId);

    /** Records the timetable used by the user on a date; the latest location wins. */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prayer.user_day (user_id, day, schedule_day_id) VALUES (:userId, :day, :scheduleDayId)
            ON CONFLICT (user_id, day) DO UPDATE SET schedule_day_id = EXCLUDED.schedule_day_id""",
            nativeQuery = true)
    void upsert(UUID userId, LocalDate day, UUID scheduleDayId);
}
