package com.yourlifeone.prayer.domain;

import lombok.Builder;

/**
 * Outcome counts for a period.
 *
 * @param classifiedTotal on-time + late + missed
 * @param opportunities every counted prayer, including pending and unclassified ones
 */
@Builder
public record OutcomeTally(
        int onTime,
        int late,
        int missed,
        int inferredMissed,
        int unclassified,
        int pending,
        int classifiedTotal,
        int opportunities,
        OutcomePercentages percentages) {
}
