package com.yourlifeone.prayer.application.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * What a {@code prayer.reminder} (deadline) or {@code prayer.notice} (prayer opportunity, momentum) event tells
 * the user's open tabs, which show it as a notification and a banner. Same fields as a reminder from
 * {@code GET /api/prayer/v1/reminders}.
 *
 * @param key names the reminder, e.g. to snooze it
 * @param kind {@code deadline}, {@code prayer-opportunity} or {@code momentum}
 * @param prayer e.g. {@code Fajr}; for momentum, the prayer whose start it follows
 * @param pillars momentum pillars ({@code learn}, {@code move}); empty for the other kinds
 * @param deadlineAt a deadline reminder's deadline (its expiry); null for the other kinds
 * @param deadline the timing that ends a deadline reminder's window, e.g. {@code Sunrise}; null otherwise
 * @param timeZone the timetable's time zone, in which times are shown
 */
@Builder
public record ReminderEventData(
        String key,
        String kind,
        LocalDate date,
        String prayer,
        List<String> pillars,
        Instant firesAt,
        Instant expiresAt,
        Instant deadlineAt,
        String deadline,
        String timeZone,
        int reminderMinutes,
        Instant snoozedUntil,
        int snoozeCount) {
}
