package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static com.yourlifeone.prayer.domain.Fixtures.LONDON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** A reminder has one snooze, which must end before the reminder does. */
class SnoozeRulesTest {
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-25T12:22:00Z");
    private static final SentReminder DHUHR = SentReminder.builder()
            .kind(ReminderKind.OPPORTUNITY).date(DAY).prayer(PrayerName.DHUHR)
            .firesAt(Instant.parse("2026-09-25T11:52:00Z")).expiresAt(EXPIRES_AT).zone(LONDON).reminderMinutes(15)
            .build();

    @Test
    void theFirstSnoozeThatEndsBeforeTheReminderIsAllowed() {
        var until = EXPIRES_AT.minusSeconds(1);

        assertEquals(Optional.empty(), SnoozeRules.refusal(DHUHR, until));
        var snoozed = SnoozeRules.snoozed(DHUHR, until);
        assertEquals(1, snoozed.snoozeCount());
        assertEquals(until, snoozed.snoozedUntil());
    }

    @Test
    void aSecondSnoozeIsRefused() {
        var snoozed = SnoozeRules.snoozed(DHUHR, EXPIRES_AT.minusSeconds(600));

        assertEquals(Optional.of(SnoozeRefusal.ALREADY_USED), SnoozeRules.refusal(snoozed, EXPIRES_AT.minusSeconds(1)));
    }

    @Test
    void aSnoozeLastingUntilTheReminderEndsIsTooLate() {
        assertEquals(Optional.of(SnoozeRefusal.TOO_LATE), SnoozeRules.refusal(DHUHR, EXPIRES_AT));
    }
}
