package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.config.PrayerProperties;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.Tracking;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.PrayerProfileRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's prayer preferences and tracking state. The first request creates them. */
@Service
@RequiredArgsConstructor
public class PrayerProfileService {
    private final PrayerProfileRepository profiles;
    private final PrayerPersistenceMapper mapper;
    private final PrayerProperties properties;
    private final Clock clock;

    /** Returns the profile, creating it (tracking starts now) on first use. */
    public PrayerProfile getOrCreate(UUID userId) {
        profiles.insertIfAbsent(userId, properties.defaultReminderMinutes(), clock.instant());
        return mapper.toDomain(profiles.findById(userId).orElseThrow());
    }

    @Transactional
    public PrayerProfile updatePreferences(UUID userId, boolean enabled, boolean reminderEnabled, int reminderMinutes) {
        requireAllowedReminderMinutes(reminderMinutes);
        getOrCreate(userId);
        var profile = profiles.findById(userId).orElseThrow();
        profile.setEnabled(enabled);
        profile.setReminderEnabled(reminderEnabled);
        profile.setReminderMinutes(reminderMinutes);
        profile.setUpdatedAt(clock.instant());
        return mapper.toDomain(profiles.save(profile));
    }

    /** Stores the activation-day eligibility once; later calls leave it unchanged. */
    @Transactional
    public void saveActivation(UUID userId, Tracking tracking) {
        var profile = profiles.findById(userId).orElseThrow();
        if (profile.getActivationDate() != null) return;
        mapper.copyTracking(tracking, profile);
        profiles.save(profile);
    }

    private void requireAllowedReminderMinutes(int reminderMinutes) {
        if (!properties.reminderMinutesOptions().contains(reminderMinutes)) {
            throw ApiException.badRequest("invalid_reminder_minutes",
                    "reminderMinutes must be one of " + properties.reminderMinutesOptions() + ".");
        }
    }
}
