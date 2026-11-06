package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Builder;

/**
 * A deadline reminder: the prayer is not recorded yet and its on-time window closes soon.
 *
 * @param deadline the timing that ends the window, e.g. {@link Timing#SUNRISE} for Fajr
 * @param zone the timetable's time zone, in which the app shows the deadline
 */
@Builder
public record Reminder(LocalDate date, PrayerName prayer, Instant deadlineAt, Timing deadline, ZoneId zone,
        int reminderMinutes) {
}
