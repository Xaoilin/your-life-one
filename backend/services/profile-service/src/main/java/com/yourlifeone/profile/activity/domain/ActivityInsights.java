package com.yourlifeone.profile.activity.domain;

import java.util.List;
import lombok.Builder;

/**
 * Everything the Activity page shows for one set of filters.
 *
 * @param totalEventCount every event the user has, whatever the filters
 * @param features every feature the user has events for, sorted, for the feature filter
 * @param coldStart true while there is too little activity to draw conclusions
 */
@Builder
public record ActivityInsights(
        int rangeDays,
        long totalEventCount,
        List<String> features,
        UsageSummary summary,
        List<UsageTrend> trends,
        List<FunnelStage> funnel,
        List<ErrorSummary> errors,
        List<Recommendation> recommendations,
        boolean coldStart) {
}
