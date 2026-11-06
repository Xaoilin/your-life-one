package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static com.yourlifeone.prayer.domain.Fixtures.london;
import static com.yourlifeone.prayer.domain.Fixtures.londonTimes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleDayTest {
    @Test
    void eachPrayerIsOnTimeUntilItsDeadlineTiming() {
        var fajr = london(DAY).window(PrayerName.FAJR).orElseThrow();
        assertEquals(Instant.parse("2026-09-25T04:12:00Z"), fajr.startsAt());
        assertEquals(Timing.SUNRISE, fajr.deadline());
        assertEquals(Instant.parse("2026-09-25T05:51:00Z"), fajr.deadlineAt());

        var dhuhr = london(DAY).window(PrayerName.DHUHR).orElseThrow();
        assertEquals(Timing.ASR, dhuhr.deadline());
        assertEquals(Instant.parse("2026-09-25T15:08:00Z"), dhuhr.deadlineAt());
    }

    @Test
    void ishaRunsPastCalendarMidnightUntilTheIslamicMidnight() {
        var isha = london(DAY).window(PrayerName.ISHA).orElseThrow();

        assertEquals(Instant.parse("2026-09-25T19:18:00Z"), isha.startsAt());
        assertEquals(Instant.parse("2026-09-25T23:52:00Z"), isha.deadlineAt());
    }

    @Test
    void aNonIncreasingWindowOtherThanIshaIsUnusable() {
        var times = londonTimes();
        times.put(Timing.ASR, LocalTime.of(12, 0));
        var day = london(DAY).toBuilder().times(times).build();

        assertTrue(day.window(PrayerName.DHUHR).isEmpty());
    }

    @Test
    void validatesTheNaturalOrderOfTimings() {
        assertTrue(TimetableValidator.isValid(londonTimes()));

        var swapped = londonTimes();
        swapped.put(Timing.SUNRISE, LocalTime.of(4, 0));
        assertFalse(TimetableValidator.isValid(swapped));

        var missing = londonTimes();
        missing.remove(Timing.MIDNIGHT);
        assertFalse(TimetableValidator.isValid(missing));
    }
}
