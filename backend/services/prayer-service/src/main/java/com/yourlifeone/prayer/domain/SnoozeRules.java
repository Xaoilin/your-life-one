package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * A reminder may be snoozed once, and only while the snooze ends before the reminder expires, as in the
 * React app's {@code canSnoozeBoundedReminder} (for deadline reminders: more than the snooze length before
 * the deadline).
 */
public final class SnoozeRules {
    private static final int SNOOZES_ALLOWED = 1;

    private SnoozeRules() {
    }

    public static Optional<SnoozeRefusal> refusal(SentReminder reminder, Instant snoozedUntil) {
        if (reminder.snoozeCount() >= SNOOZES_ALLOWED) return Optional.of(SnoozeRefusal.ALREADY_USED);
        if (!snoozedUntil.isBefore(reminder.expiresAt())) return Optional.of(SnoozeRefusal.TOO_LATE);
        return Optional.empty();
    }

    /** The reminder after its snooze. */
    public static SentReminder snoozed(SentReminder reminder, Instant snoozedUntil) {
        return reminder.toBuilder().snoozedUntil(snoozedUntil).snoozeCount(SNOOZES_ALLOWED).build();
    }
}
