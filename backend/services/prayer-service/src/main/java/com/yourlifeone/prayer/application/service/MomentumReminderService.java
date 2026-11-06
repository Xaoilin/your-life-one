package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.config.ReminderProperties;
import com.yourlifeone.prayer.domain.MomentumPreference;
import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.ReminderPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.MomentumReminderRepository;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The momentum reminder preferences the app sends whenever Daily Momentum changes: which prayers each
 * pillar prompts after, and the latest date its Level 1 was complete. A pillar never sent uses the defaults.
 */
@Service
@RequiredArgsConstructor
public class MomentumReminderService {
    private final MomentumReminderRepository preferences;
    private final ReminderPersistenceMapper mapper;
    private final ReminderProperties properties;
    private final Clock clock;

    /** Every pillar's preference, in pillar order. */
    public List<MomentumPreference> preferences(UUID userId) {
        var stored = preferences.findByUserId(userId).stream().map(mapper::toDomain)
                .collect(Collectors.toMap(MomentumPreference::pillar, Function.identity()));
        return Arrays.stream(Pillar.values()).map(pillar -> stored.getOrDefault(pillar, defaults(pillar))).toList();
    }

    /** Replaces the given pillars' preferences; the others stay as they were. */
    @Transactional
    public List<MomentumPreference> save(UUID userId, List<MomentumPreference> changed) {
        requireOncePerPillar(changed);
        changed.forEach(MomentumReminderService::requireDistinctPrayers);
        var now = clock.instant();
        preferences.saveAll(changed.stream().map(preference -> mapper.toEntity(userId, preference, now)).toList());
        return preferences(userId);
    }

    private MomentumPreference defaults(Pillar pillar) {
        return MomentumPreference.builder()
                .pillar(pillar)
                .enabled(properties.defaultMomentumEnabled())
                .afterPrayers(properties.defaultAnchors(pillar))
                .build();
    }

    private static void requireOncePerPillar(List<MomentumPreference> changed) {
        var pillars = new HashSet<Pillar>();
        for (var preference : changed) {
            if (!pillars.add(preference.pillar())) {
                throw ApiException.badRequest("duplicate_pillar", "Each pillar may appear once.");
            }
        }
    }

    private static void requireDistinctPrayers(MomentumPreference preference) {
        if (new HashSet<>(preference.afterPrayers()).size() != preference.afterPrayers().size()) {
            throw ApiException.badRequest("duplicate_prayer", "afterPrayers may name each prayer once.");
        }
    }
}
