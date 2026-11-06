package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * A bounded notice the timetable and preferences call for: active from {@code firesAt} until
 * {@code expiresAt}.
 *
 * @param prayer the prayer whose start it follows
 * @param pillars the momentum pillars it prompts, in pillar order; empty for a prayer opportunity
 */
@Builder
public record PlannedNotice(ReminderKind kind, LocalDate date, PrayerName prayer, List<Pillar> pillars,
        Instant firesAt, Instant expiresAt) {
    public PlannedNotice {
        pillars = pillars == null ? List.of() : List.copyOf(pillars);
    }

    public String key() {
        return ReminderKeys.of(kind, date, prayer, firesAt, pillars);
    }

    /** One receipt per pillar of a momentum notice; a prayer opportunity has just its own. */
    public List<String> receiptKeys() {
        if (kind != ReminderKind.MOMENTUM) return List.of(key());
        return pillars.stream().map(pillar -> ReminderKeys.of(kind, date, prayer, firesAt, List.of(pillar))).toList();
    }

    public boolean isActiveAt(Instant now) {
        return !firesAt.isAfter(now) && now.isBefore(expiresAt);
    }
}
