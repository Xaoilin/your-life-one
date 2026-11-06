package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.ScheduleDayEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ScheduleDayRepository extends JpaRepository<ScheduleDayEntity, UUID> {
    @Query("""
            SELECT s FROM ScheduleDayEntity s
            WHERE lower(s.city) = lower(:city) AND lower(s.country) = lower(:country)
              AND s.method = :method AND s.scheduleDate = :date""")
    Optional<ScheduleDayEntity> findForDate(String city, String country, int method, LocalDate date);

    /** Any cached timetable for the location; used to learn its time zone. */
    Optional<ScheduleDayEntity> findFirstByCityIgnoreCaseAndCountryIgnoreCaseAndMethodOrderByScheduleDateDesc(
            String city, String country, int method);

    /** Caches a timetable unless another request already cached the same location and date. */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prayer.schedule_day (id, city, country, method, schedule_date, time_zone, hijri_date,
                fajr, sunrise, dhuhr, asr, sunset, maghrib, isha, midnight, fetched_at)
            VALUES (:#{#s.id}, :#{#s.city}, :#{#s.country}, :#{#s.method}, :#{#s.scheduleDate}, :#{#s.timeZone},
                :#{#s.hijriDate}, :#{#s.fajr}, :#{#s.sunrise}, :#{#s.dhuhr}, :#{#s.asr}, :#{#s.sunset},
                :#{#s.maghrib}, :#{#s.isha}, :#{#s.midnight}, :#{#s.fetchedAt})
            ON CONFLICT (lower(city), lower(country), method, schedule_date) DO NOTHING""", nativeQuery = true)
    void insertIfAbsent(ScheduleDayEntity s);
}
