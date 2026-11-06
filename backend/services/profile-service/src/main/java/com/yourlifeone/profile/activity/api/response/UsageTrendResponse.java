package com.yourlifeone.profile.activity.api.response;

import lombok.Builder;

/** @param changePercent null when the path did not occur in the previous window */
@Builder
public record UsageTrendResponse(
        String key,
        String feature,
        String action,
        long currentCount,
        long previousCount,
        Integer changePercent) {
}
