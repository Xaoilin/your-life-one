package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.PreferencesProperties;
import com.yourlifeone.profile.domain.AppPreferences;
import com.yourlifeone.profile.infrastructure.persistence.entity.AppPreferencesEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.AppPreferencesRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** App preferences every device shares. A save replaces the whole value. */
@Service
@RequiredArgsConstructor
public class AppPreferencesService {
    private final AppPreferencesRepository preferences;
    private final PreferencesProperties properties;
    private final Clock clock;

    /** The user's preferences, or the configured defaults until they save some. */
    @Transactional(readOnly = true)
    public AppPreferences get(UUID userId) {
        return preferences.findById(userId).map(AppPreferencesService::toDomain).orElseGet(this::defaults);
    }

    @Transactional
    public AppPreferences update(UUID userId, AppPreferences requested) {
        requireAllowed(requested);
        var entity = preferences.findById(userId).orElseGet(() -> AppPreferencesEntity.builder().userId(userId).build());
        entity.setTheme(requested.theme());
        entity.setDataRetentionDays(requested.dataRetentionDays());
        entity.setTelemetry(requested.telemetry());
        entity.setDefaultCalendarTab(requested.defaultCalendarTab());
        entity.getGoalTags().clear();
        entity.getGoalTags().addAll(trimmedTags(requested.goalTags()));
        entity.setUpdatedAt(clock.instant());
        return toDomain(preferences.save(entity));
    }

    private void requireAllowed(AppPreferences requested) {
        if (!properties.themes().contains(requested.theme())) {
            throw ApiException.badRequest("invalid_theme", "theme must be one of " + properties.themes() + ".");
        }
        var tab = requested.defaultCalendarTab();
        if (tab != null && !properties.calendarTabs().contains(tab)) {
            throw ApiException.badRequest("invalid_calendar_tab",
                    "defaultCalendarTab must be one of " + properties.calendarTabs() + ".");
        }
        int days = requested.dataRetentionDays();
        if (days < properties.minDataRetentionDays() || days > properties.maxDataRetentionDays()) {
            throw ApiException.badRequest("invalid_data_retention", "dataRetentionDays must be between "
                    + properties.minDataRetentionDays() + " and " + properties.maxDataRetentionDays() + ".");
        }
        requireValidTags(trimmedTags(requested.goalTags()));
    }

    private void requireValidTags(List<String> tags) {
        if (tags.size() > properties.maxGoalTags()) {
            throw ApiException.badRequest("too_many_goal_tags",
                    "At most " + properties.maxGoalTags() + " goal tags are allowed.");
        }
        var seen = new HashSet<String>();
        for (var tag : tags) {
            if (tag.isEmpty() || tag.length() > properties.maxGoalTagLength()) {
                throw ApiException.badRequest("invalid_goal_tag",
                        "Goal tags must be 1 to " + properties.maxGoalTagLength() + " characters.");
            }
            if (!seen.add(tag)) {
                throw ApiException.badRequest("duplicate_goal_tag", "Goal tag \"" + tag + "\" appears more than once.");
            }
        }
    }

    private static List<String> trimmedTags(List<String> tags) {
        return tags == null ? List.of() : tags.stream().map(String::trim).toList();
    }

    private AppPreferences defaults() {
        return AppPreferences.builder()
                .theme(properties.defaultTheme())
                .dataRetentionDays(properties.defaultDataRetentionDays())
                .telemetry(false)
                .goalTags(List.of())
                .build();
    }

    private static AppPreferences toDomain(AppPreferencesEntity entity) {
        return AppPreferences.builder()
                .theme(entity.getTheme())
                .dataRetentionDays(entity.getDataRetentionDays())
                .telemetry(entity.isTelemetry())
                .defaultCalendarTab(entity.getDefaultCalendarTab())
                .goalTags(List.copyOf(new ArrayList<>(entity.getGoalTags())))
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
