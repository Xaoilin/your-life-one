package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.ReminderInputs;
import com.yourlifeone.prayer.config.ReminderProperties;
import com.yourlifeone.prayer.domain.BoundedReminderPlanner;
import com.yourlifeone.prayer.domain.MomentumPreference;
import com.yourlifeone.prayer.domain.PlannedNotice;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.ReminderPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.NoticeRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sends today's bounded notices (prayer opportunities and momentum prompts) once each: a notice goes out
 * while it is active if one of its receipts was never recorded and none of them is snoozed into the future,
 * as the React app's {@code getAttemptableBoundedReminders} decided.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository notices;
    private final ReminderPersistenceMapper mapper;
    private final ReminderProperties properties;

    /** The notices due now that were not sent before, each now recorded as sent. */
    public List<SentReminder> claimDue(UUID userId, PrayerProfile profile, List<MomentumPreference> preferences,
            Instant now, ReminderInputs inputs) {
        var today = inputs.today();
        if (today.isEmpty()) return List.of();
        var day = today.get();
        var recorded = inputs.recorded().onDate(day.date());
        var plans = BoundedReminderPlanner.plan(day, recorded, preferences, profile.reminderEnabled(), properties.policy());
        return BoundedReminderPlanner.activeAt(plans, now).stream()
                .filter(plan -> claim(userId, plan, now))
                .map(plan -> sent(plan, day.zone(), profile.reminderMinutes()))
                .toList();
    }

    private boolean claim(UUID userId, PlannedNotice plan, Instant now) {
        var existing = notices.findByUserIdAndKeyIn(userId, plan.receiptKeys());
        var snoozed = existing.stream().anyMatch(receipt -> receipt.getSnoozedUntil() != null
                && receipt.getSnoozedUntil().isAfter(now));
        if (snoozed || existing.size() == plan.receiptKeys().size()) return false;
        var recorded = 0;
        for (var receipt : mapper.toReceipts(userId, plan, now)) recorded += notices.insertIfAbsent(receipt);
        return recorded > 0;
    }

    private static SentReminder sent(PlannedNotice plan, ZoneId zone, int reminderMinutes) {
        return SentReminder.builder()
                .kind(plan.kind())
                .date(plan.date())
                .prayer(plan.prayer())
                .pillars(plan.pillars())
                .firesAt(plan.firesAt())
                .expiresAt(plan.expiresAt())
                .zone(zone)
                .reminderMinutes(reminderMinutes)
                .build();
    }
}
