package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.ReminderKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * A sent reminder, with the same fields as the {@code prayer.reminder} and {@code prayer.notice} event data.
 *
 * @param deadlineAt a deadline reminder's deadline; null for the other kinds
 * @param deadline the timing that ends a deadline reminder's window, e.g. {@code Sunrise}; null otherwise
 */
@Builder
public record ReminderResponse(
        String key,
        ReminderKind kind,
        LocalDate date,
        PrayerName prayer,
        List<Pillar> pillars,
        Instant firesAt,
        Instant expiresAt,
        Instant deadlineAt,
        String deadline,
        String timeZone,
        int reminderMinutes,
        Instant snoozedUntil,
        int snoozeCount) {
}
