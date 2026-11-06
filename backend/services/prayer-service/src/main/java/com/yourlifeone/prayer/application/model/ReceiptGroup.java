package com.yourlifeone.prayer.application.model;

import com.yourlifeone.prayer.domain.SentReminder;
import java.time.Instant;
import java.util.List;

/**
 * A sent reminder with the receipts it was recorded under.
 *
 * @param noticeKeys the notice receipt keys (one per pillar of a momentum notice); empty for a deadline reminder
 * @param resent whether its one re-send after a snooze already went out
 */
public record ReceiptGroup(SentReminder reminder, List<String> noticeKeys, boolean resent) {
    public ReceiptGroup {
        noticeKeys = List.copyOf(noticeKeys);
    }

    /** Its snooze has ended and it has not been sent again yet. */
    public boolean dueForResend(Instant now) {
        var snoozedUntil = reminder.snoozedUntil();
        return !resent && snoozedUntil != null && !snoozedUntil.isAfter(now) && now.isBefore(reminder.expiresAt());
    }
}
