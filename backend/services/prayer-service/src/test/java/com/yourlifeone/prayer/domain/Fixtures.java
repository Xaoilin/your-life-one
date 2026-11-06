package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** London, 25 September 2026 (BST, UTC+1): the AlAdhan method-0 timetable. */
public final class Fixtures {
    public static final LocalDate DAY = LocalDate.parse("2026-09-25");
    public static final ZoneId LONDON = ZoneId.of("Europe/London");

    private Fixtures() {
    }

    public static Map<Timing, LocalTime> londonTimes() {
        var times = new EnumMap<Timing, LocalTime>(Timing.class);
        times.put(Timing.FAJR, LocalTime.of(5, 12));
        times.put(Timing.SUNRISE, LocalTime.of(6, 51));
        times.put(Timing.DHUHR, LocalTime.of(12, 52));
        times.put(Timing.ASR, LocalTime.of(16, 8));
        times.put(Timing.SUNSET, LocalTime.of(18, 52));
        times.put(Timing.MAGHRIB, LocalTime.of(19, 12));
        times.put(Timing.ISHA, LocalTime.of(20, 18));
        times.put(Timing.MIDNIGHT, LocalTime.of(0, 52));
        return times;
    }

    public static ScheduleDay london(LocalDate date) {
        return ScheduleDay.builder()
                .id(UUID.randomUUID())
                .city("London")
                .country("United Kingdom")
                .date(date)
                .zone(LONDON)
                .hijriDate("14 Rabīʿ al-thānī 1448")
                .times(londonTimes())
                .fetchedAt(Instant.parse("2026-09-25T00:00:00Z"))
                .build();
    }

    public static Outcome outcome(LocalDate date, PrayerName prayer, OutcomeStatus status, Instant recordedAt) {
        return Outcome.builder()
                .id(UUID.randomUUID())
                .date(date)
                .prayer(prayer)
                .status(status)
                .recordedAt(recordedAt)
                .source("dashboard")
                .rewarded(status.isCompleted())
                .build();
    }
}
