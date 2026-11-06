package com.yourlifeone.profile.activity.api.response;

import lombok.Builder;

/** @param failureRate null when no event in the window reports an outcome */
@Builder
public record UsageSummaryResponse(
        long eventCount,
        long sessionCount,
        long activeSurfaceCount,
        long errorCount,
        Integer failureRate) {
}
