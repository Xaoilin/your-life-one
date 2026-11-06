package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.PrayerProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Claims one account's receipts atomically with its Auth identity check; publication follows commit. */
@Service
@RequiredArgsConstructor
public class ReminderEvaluationService {
    private final PrayerProfileRepository profiles;
    private final PrayerPersistenceMapper mapper;
    private final ReminderService deadlines;
    private final NoticeService notices;
    private final ReminderResendService resends;
    private final MomentumReminderService momentum;
    private final ReminderDays days;

    @Transactional
    public List<SentReminder> claimDue(UUID userId, Instant now) {
        if (profiles.lockIdentity(userId).isEmpty()) return List.of();
        return profiles.findById(userId).filter(entity -> entity.isEnabled())
                .map(entity -> due(userId, mapper.toDomain(entity), now)).orElse(List.of());
    }

    private List<SentReminder> due(UUID userId, PrayerProfile profile, Instant now) {
        var preferences = momentum.preferences(userId);
        var inputs = days.forPass(userId, now, profile.reminderEnabled());
        var due = new ArrayList<SentReminder>();
        if (profile.reminderEnabled()) due.addAll(deadlines.claimDue(userId, profile.reminderMinutes(), now, inputs));
        due.addAll(notices.claimDue(userId, profile, preferences, now, inputs));
        due.addAll(resends.claimDue(userId, profile, preferences, now, inputs));
        return due;
    }
}
