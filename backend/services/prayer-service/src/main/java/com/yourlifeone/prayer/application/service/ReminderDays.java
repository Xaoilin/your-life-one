package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.ReminderInputs;
import com.yourlifeone.prayer.domain.RecordedPrayers;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.OutcomeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The timetables and recorded prayers reminders are decided from. Timetables are the ones recorded for the
 * user (the app records today's when it loads); a date without one reuses the user's latest, as the app
 * always did.
 */
@Component
@RequiredArgsConstructor
public class ReminderDays {
    private final UserScheduleService userSchedules;
    private final OutcomeRepository outcomes;
    private final PrayerPersistenceMapper mapper;

    /** Loads shared schedules and outcomes once; receipt claims and snoozes remain live repository reads. */
    public ReminderInputs forPass(UUID userId, Instant now, boolean includeYesterday) {
        var latest = userSchedules.latest(userId);
        if (latest.isEmpty()) return new ReminderInputs(List.of(), RecordedPrayers.of(List.of()), ZoneOffset.UTC);
        var date = LocalDate.ofInstant(now, latest.get().zone());
        var from = includeYesterday ? date.minusDays(1) : date;
        var known = userSchedules.knownDays(userId, from, date);
        var today = known.getOrDefault(date, latest.get().onDate(date));
        var timetables = includeYesterday
                ? List.of(known.getOrDefault(from, today.onDate(from)), today) : List.of(today);
        return new ReminderInputs(timetables, recorded(userId, from, date), latest.get().zone());
    }

    /** Yesterday's and today's timetables in the user's latest time zone; none before any timetable is known. */
    public List<ScheduleDay> yesterdayAndToday(UUID userId, Instant now) {
        return today(userId, now).map(today -> List.of(dayOf(userId, today.date().minusDays(1), today), today))
                .orElse(List.of());
    }

    /** Today's timetable in the user's latest time zone. */
    public Optional<ScheduleDay> today(UUID userId, Instant now) {
        return userSchedules.latest(userId).map(latest -> dayOf(userId, LocalDate.ofInstant(now, latest.zone()), latest));
    }

    /** The time zone of the user's latest timetable, in which reminders show their times. */
    public ZoneId zone(UUID userId) {
        return userSchedules.latest(userId).map(ScheduleDay::zone).orElse(ZoneOffset.UTC);
    }

    public RecordedPrayers recorded(UUID userId, LocalDate from, LocalDate to) {
        return RecordedPrayers.of(outcomes.findByUserIdAndPrayerDateBetween(userId, from, to).stream()
                .map(mapper::toDomain)
                .toList());
    }

    private ScheduleDay dayOf(UUID userId, LocalDate date, ScheduleDay latest) {
        return userSchedules.knownDay(userId, date).orElseGet(() -> latest.onDate(date));
    }
}
