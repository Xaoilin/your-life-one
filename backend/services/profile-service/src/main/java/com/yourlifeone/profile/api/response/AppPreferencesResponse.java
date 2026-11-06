package com.yourlifeone.profile.api.response;

import java.time.Instant;
import java.util.List;

public record AppPreferencesResponse(
        String theme,
        int dataRetentionDays,
        boolean telemetry,
        String defaultCalendarTab,
        List<String> goalTags,
        Instant updatedAt) {
}
