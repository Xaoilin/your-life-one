package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.config.ReminderProperties;
import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.ReminderKey;
import com.yourlifeone.prayer.domain.ReminderKeys;
import com.yourlifeone.prayer.domain.ReminderKind;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.domain.SnoozeRefusal;
import com.yourlifeone.prayer.domain.SnoozeRules;
import com.yourlifeone.prayer.infrastructure.persistence.entity.NoticeEntity;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.ReminderPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.NoticeRepository;
import com.yourlifeone.prayer.infrastructure.persistence.repository.ReminderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Snoozes a sent reminder once, for the configured length: its receipts are locked, so two snoozes at once
 * cannot both succeed. When the snooze ends the scheduler sends it again, once.
 */
@Service
@RequiredArgsConstructor
public class ReminderSnoozeService {
    private final ReminderRepository reminders;
    private final NoticeRepository notices;
    private final ReminderPersistenceMapper mapper;
    private final PrayerProfileService profiles;
    private final ReminderDays days;
    private final ReminderProperties properties;
    private final Clock clock;

    /**
     * @throws ApiException 404 {@code reminder_not_found} for an unknown or expired key, 409 {@code snooze_used}
     *         after its one snooze, 409 {@code snooze_too_late} when the snooze would outlast it
     */
    @Transactional
    public SentReminder snooze(UUID userId, String key) {
        var parsed = ReminderKeys.parse(key).orElseThrow(ReminderSnoozeService::notFound);
        var now = clock.instant();
        var minutes = profiles.getOrCreate(userId).reminderMinutes();
        return parsed.kind() == ReminderKind.DEADLINE
                ? snoozeDeadline(userId, parsed, minutes, now)
                : snoozeNotice(userId, parsed, minutes, now);
    }

    private SentReminder snoozeDeadline(UUID userId, ReminderKey key, int minutes, Instant now) {
        var receipt = reminders.findForUpdate(userId, key.date(), key.prayer().label())
                .orElseThrow(ReminderSnoozeService::notFound);
        var snoozed = snoozed(mapper.toSentReminder(receipt, days.zone(userId), minutes), now);
        receipt.setSnoozedUntil(snoozed.snoozedUntil());
        receipt.setSnoozeCount(snoozed.snoozeCount());
        reminders.save(receipt);
        return snoozed;
    }

    private SentReminder snoozeNotice(UUID userId, ReminderKey key, int minutes, Instant now) {
        var receipts = noticeReceipts(userId, key);
        var snoozed = snoozed(mapper.toSentReminder(receipts, days.zone(userId), minutes), now);
        receipts.forEach(receipt -> {
            receipt.setSnoozedUntil(snoozed.snoozedUntil());
            receipt.setSnoozeCount(snoozed.snoozeCount());
        });
        notices.saveAll(receipts);
        return snoozed;
    }

    /** The notice's receipts, locked; every pillar a momentum key names must have one. */
    private List<NoticeEntity> noticeReceipts(UUID userId, ReminderKey key) {
        var kind = key.kind().label();
        if (key.kind() == ReminderKind.OPPORTUNITY) {
            var receipts = notices.findPrayerNoticesForUpdate(userId, kind, key.date(), key.prayer().label());
            if (receipts.isEmpty()) throw notFound();
            return List.of(receipts.getFirst());
        }
        var pillars = key.pillars().stream().map(Pillar::label).toList();
        var receipts = notices.findMomentumNoticesForUpdate(userId, kind, key.date(), key.firesAt(), pillars);
        if (receipts.size() != pillars.size()) throw notFound();
        return receipts;
    }

    private SentReminder snoozed(SentReminder reminder, Instant now) {
        if (!now.isBefore(reminder.expiresAt())) throw notFound();
        var snoozedUntil = now.plus(properties.snooze());
        SnoozeRules.refusal(reminder, snoozedUntil).ifPresent(refusal -> {
            throw refused(refusal);
        });
        return SnoozeRules.snoozed(reminder, snoozedUntil);
    }

    private static ApiException refused(SnoozeRefusal refusal) {
        return switch (refusal) {
            case ALREADY_USED -> ApiException.conflict("snooze_used", "This reminder has already used its one snooze.");
            case TOO_LATE -> ApiException.conflict("snooze_too_late", "This reminder ends before the snooze would.");
        };
    }

    private static ApiException notFound() {
        return ApiException.notFound("reminder_not_found", "No such reminder is active.");
    }
}
