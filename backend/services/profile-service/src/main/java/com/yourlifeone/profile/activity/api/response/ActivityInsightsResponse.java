package com.yourlifeone.profile.activity.api.response;

import java.util.List;
import lombok.Builder;

/**
 * Everything the Activity page shows for the chosen filters.
 *
 * @param totalEventCount every event the user has, whatever the filters (zero: no activity yet)
 * @param features every feature the user has events for, sorted, for the feature filter
 * @param coldStart true while there are too few events or sessions to draw conclusions
 */
@Builder
public record ActivityInsightsResponse(
        int rangeDays,
        long totalEventCount,
        List<String> features,
        UsageSummaryResponse summary,
        List<UsageTrendResponse> trends,
        List<FunnelStageResponse> funnel,
        List<ErrorSummaryResponse> errors,
        List<RecommendationResponse> recommendations,
        boolean coldStart) {
}
