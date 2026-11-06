package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.profile.activity.config.ActivityIngestProperties;
import com.yourlifeone.profile.activity.config.ActivityInsightsProperties;
import java.util.List;

/** The activity settings the service ships with (see application.yml). */
final class ActivityTestProperties {
    static final int MAX_BATCH = 25;
    static final ActivityIngestProperties INGEST = new ActivityIngestProperties(1, MAX_BATCH, 1_000_000, 1_800_000,
            List.of("session", "navigation", "action", "outcome", "error", "performance"),
            List.of("dashboard", "calendar", "tasks", "activity"),
            List.of("success", "failure", "cancelled", "unavailable"),
            List.of("mobile", "tablet", "desktop"),
            List.of("pointer", "keyboard", "voice", "assistant", "system"),
            List.of("previousSurface", "navigationSource", "viewportBucket", "visibilityState", "retryCount"),
            2048, 96,
            "[a-z][a-z0-9_]{0,63}", "[a-z][a-z0-9_:-]{0,63}", "[a-z][a-z0-9_:-]{0,95}",
            "[0-9]+\\.[0-9]+\\.[0-9]+([+-][a-zA-Z0-9.-]+)?", 32);
    static final ActivityInsightsProperties INSIGHTS = new ActivityInsightsProperties(List.of(7, 30, 90), 30, 8, 8, 3);

    private ActivityTestProperties() {
    }
}
