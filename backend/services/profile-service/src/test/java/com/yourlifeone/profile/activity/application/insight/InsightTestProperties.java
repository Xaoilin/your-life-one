package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.config.ActivityInsightsProperties;
import java.util.List;

/** The insight limits the service ships with. */
final class InsightTestProperties {
    static final int MAX_TRENDS = 8;
    static final int MAX_ERRORS = 8;
    static final int MAX_RECOMMENDATIONS = 3;
    static final ActivityInsightsProperties DEFAULTS =
            new ActivityInsightsProperties(List.of(7, 30, 90), 30, MAX_TRENDS, MAX_ERRORS, MAX_RECOMMENDATIONS);

    private InsightTestProperties() {
    }
}
