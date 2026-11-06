package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;

/**
 * One location's published timings for one date.
 *
 * @param id null until stored
 */
@Builder(toBuilder = true)
public record ScheduleDay(
        UUID id,
        String city,
        String country,
        LocalDate date,
        ZoneId zone,
        String hijriDate,
        Map<Timing, LocalTime> times,
        Instant fetchedAt) {

    public static final int METHOD_ID = 0;
    public static final String METHOD_NAME = "Shia Ithna-Ashari, Leva Institute, Qum";

    public ScheduleDay {
        times = Collections.unmodifiableMap(new EnumMap<>(times));
    }

    /** The same timetable applied to another date (used when that date's own timetable is unknown). */
    public ScheduleDay onDate(LocalDate otherDate) {
        return toBuilder().date(otherDate).build();
    }

    /**
     * The prayer's on-time window, from its own timing to its deadline timing. Only Isha's
     * Midnight deadline may fall on the next calendar date; any other window that does not move
     * forward in time means the timetable is unusable for that prayer.
     */
    public Optional<PrayerWindow> window(PrayerName prayer) {
        var start = times.get(prayer.timing());
        var deadline = times.get(prayer.deadline());
        if (start == null || deadline == null) return Optional.empty();

        var startsAt = instantOn(date, start);
        var deadlineAt = instantOn(date, deadline);
        if (!deadlineAt.isAfter(startsAt)) {
            if (prayer.deadline() != Timing.MIDNIGHT) return Optional.empty();
            deadlineAt = instantOn(date.plusDays(1), deadline);
        }
        return Optional.of(PrayerWindow.builder()
                .prayer(prayer)
                .startsAt(startsAt)
                .deadline(prayer.deadline())
                .deadlineAt(deadlineAt)
                .build());
    }

    private Instant instantOn(LocalDate day, LocalTime time) {
        return ZonedDateTime.of(day, time, zone).toInstant();
    }
}
