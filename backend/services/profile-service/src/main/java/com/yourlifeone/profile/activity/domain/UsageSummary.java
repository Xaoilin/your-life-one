package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * The headline numbers for the filtered current window.
 *
 * @param failureRate errors as a whole-number percentage of events that report an outcome; null when none do
 */
@Builder
public record UsageSummary(
        long eventCount,
        long sessionCount,
        long activeSurfaceCount,
        long errorCount,
        Integer failureRate) {
}
