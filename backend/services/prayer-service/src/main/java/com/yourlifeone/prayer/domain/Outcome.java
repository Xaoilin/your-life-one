package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

/**
 * A recorded prayer outcome; at most one per user, date and prayer.
 *
 * @param rewarded persistent one-time reward receipt; corrections never clear it
 * @param deadlineAt the on-time deadline in force when recorded, if the timetable was known
 */
@Builder(toBuilder = true)
public record Outcome(
        UUID id,
        LocalDate date,
        PrayerName prayer,
        OutcomeStatus status,
        OutcomeTiming timing,
        Instant recordedAt,
        String source,
        String taskId,
        boolean rewarded,
        Instant deadlineAt) {
}
