package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.Builder;

/**
 * A reminder the service sent, as the app shows it.
 *
 * @param prayer the prayer it is about; for momentum, the prayer whose start it follows
 * @param pillars the momentum pillars it prompts; empty for the other kinds
 * @param firesAt when it became due
 * @param expiresAt when it stops being shown: the deadline, or the end of a bounded notice
 * @param deadline the timing that ends a deadline reminder's window, e.g. {@link Timing#SUNRISE}; else null
 * @param zone the timetable's time zone, in which the app shows times
 * @param snoozedUntil when its one snooze ends; null if never snoozed
 */
@Builder(toBuilder = true)
public record SentReminder(
        ReminderKind kind,
        LocalDate date,
        PrayerName prayer,
        List<Pillar> pillars,
        Instant firesAt,
        Instant expiresAt,
        Timing deadline,
        ZoneId zone,
        int reminderMinutes,
        Instant snoozedUntil,
        int snoozeCount) {

    public SentReminder {
        pillars = pillars == null ? List.of() : List.copyOf(pillars);
    }

    public String key() {
        return ReminderKeys.of(kind, date, prayer, firesAt, pillars);
    }

    /** Not expired and not snoozed into the future. */
    public boolean isShownAt(Instant now) {
        return now.isBefore(expiresAt) && (snoozedUntil == null || !snoozedUntil.isAfter(now));
    }
}
