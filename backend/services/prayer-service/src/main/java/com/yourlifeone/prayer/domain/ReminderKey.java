package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * A reminder key, taken apart.
 *
 * @param prayer the prayer of a deadline or opportunity reminder; null for momentum
 * @param firesAt the instant of a momentum notice; null otherwise
 * @param pillars the pillars of a momentum notice, in pillar order; empty otherwise
 */
@Builder
public record ReminderKey(ReminderKind kind, LocalDate date, PrayerName prayer, Instant firesAt, List<Pillar> pillars) {
    public ReminderKey {
        pillars = pillars == null ? List.of() : List.copyOf(pillars);
    }
}
