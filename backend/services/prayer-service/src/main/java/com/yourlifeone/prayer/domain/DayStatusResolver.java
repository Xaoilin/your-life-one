package com.yourlifeone.prayer.domain;

import java.time.Instant;

/** Decides how one prayer on one day is shown: its recorded outcome, or what its window implies. */
public final class DayStatusResolver {
    private DayStatusResolver() {
    }

    /**
     * @param outcome the recorded outcome, or null
     * @param day that day's timetable, or null when unknown
     */
    public static DayStatus resolve(Tracking tracking, Outcome outcome, ScheduleDay day, PrayerName prayer, Instant now) {
        if (outcome != null) return DayStatus.of(outcome.status());
        if (day == null) return DayStatus.PENDING;
        if (!tracking.isTracked(day, prayer, now)) return DayStatus.NOT_TRACKED;
        return hasWindowClosed(day, prayer, now) ? DayStatus.MISSED : DayStatus.PENDING;
    }

    public static boolean hasWindowClosed(ScheduleDay day, PrayerName prayer, Instant now) {
        return day.window(prayer)
                .map(window -> !now.isBefore(window.deadlineAt()))
                .orElse(false);
    }
}
