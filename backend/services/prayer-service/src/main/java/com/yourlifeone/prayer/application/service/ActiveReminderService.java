package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.ReceiptGroup;
import com.yourlifeone.prayer.domain.SentReminder;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** The reminders the app shows now: sent, not expired, not snoozed into the future and still wanted. */
@Service
@RequiredArgsConstructor
public class ActiveReminderService {
    private static final Comparator<SentReminder> NEWEST_FIRST = Comparator.comparing(SentReminder::firesAt)
            .thenComparing(SentReminder::key).reversed();

    private final ReminderReceipts receipts;
    private final PrayerProfileService profiles;
    private final MomentumReminderService momentum;
    private final Clock clock;

    public List<SentReminder> active(UUID userId) {
        var now = clock.instant();
        return receipts.unexpired(userId, profiles.getOrCreate(userId), momentum.preferences(userId), now).stream()
                .map(ReceiptGroup::reminder)
                .filter(reminder -> reminder.isShownAt(now))
                .sorted(NEWEST_FIRST)
                .toList();
    }
}
