package com.yourlifeone.profile.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * App preferences every device of an account shares.
 *
 * @param defaultCalendarTab calendar view opened by default; null uses the app's own default
 * @param goalTags the user's goal tags, in order
 * @param updatedAt null until the user first saves preferences
 */
@Builder(toBuilder = true)
public record AppPreferences(
        String theme,
        int dataRetentionDays,
        boolean telemetry,
        String defaultCalendarTab,
        List<String> goalTags,
        Instant updatedAt) {
}
