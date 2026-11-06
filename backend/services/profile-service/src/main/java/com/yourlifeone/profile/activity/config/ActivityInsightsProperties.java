package com.yourlifeone.profile.activity.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How the Activity page's insights are summarised ({@code app.profile.activity.insights.*}).
 *
 * @param rangeDays windows a user may choose, in days; trends compare with the equal window before
 * @param defaultRangeDays window when none is chosen; one of {@code rangeDays}
 * @param maxTrends most feature paths listed
 * @param maxErrors most error codes listed
 * @param maxRecommendations most recommendations given
 */
@ConfigurationProperties("app.profile.activity.insights")
public record ActivityInsightsProperties(
        List<Integer> rangeDays,
        int defaultRangeDays,
        int maxTrends,
        int maxErrors,
        int maxRecommendations) {
    public ActivityInsightsProperties {
        rangeDays = rangeDays == null ? List.of() : List.copyOf(rangeDays);
        if (!rangeDays.contains(defaultRangeDays)) {
            throw new IllegalArgumentException("app.profile.activity.insights.default-range-days must be one of range-days");
        }
    }
}
