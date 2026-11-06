package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * One of the most-used feature/action paths.
 *
 * @param key {@code feature:action}
 * @param changePercent whole-number change against the previous window; null when the path is new
 */
@Builder
public record UsageTrend(
        String key,
        String feature,
        String action,
        long currentCount,
        long previousCount,
        Integer changePercent) {
}
