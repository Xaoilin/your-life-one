package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.ReceiptGroup;
import com.yourlifeone.prayer.application.model.ReminderInputs;
import com.yourlifeone.prayer.domain.MomentumPreference;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.infrastructure.persistence.repository.NoticeRepository;
import com.yourlifeone.prayer.infrastructure.persistence.repository.ReminderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Sends a snoozed reminder once more when its snooze ends, if it is still active and wanted. */
@Service
@RequiredArgsConstructor
public class ReminderResendService {
    private final ReminderReceipts receipts;
    private final ReminderRepository reminders;
    private final NoticeRepository notices;

    /** The reminders whose snooze ended, each now recorded as sent again. */
    public List<SentReminder> claimDue(UUID userId, PrayerProfile profile, List<MomentumPreference> preferences,
            Instant now, ReminderInputs inputs) {
        return receipts.unexpired(userId, profile, preferences, now, inputs).stream()
                .filter(group -> group.dueForResend(now))
                .filter(group -> claim(userId, group, now))
                .map(ReceiptGroup::reminder)
                .toList();
    }

    private boolean claim(UUID userId, ReceiptGroup group, Instant now) {
        var reminder = group.reminder();
        if (reminder.kind() == ReminderKind.DEADLINE) {
            return reminders.claimResend(userId, reminder.date(), reminder.prayer().label(), now) == 1;
        }
        return notices.claimResend(userId, group.noticeKeys(), now) > 0;
    }
}
