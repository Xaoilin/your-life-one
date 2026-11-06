package com.yourlifeone.prayer.domain;

import java.util.Map;
import lombok.Builder;

/** Outcome statistics for a period, overall and per prayer. */
@Builder(toBuilder = true)
public record OutcomeStats(OutcomeTally overall, int trackedDays, Map<PrayerName, OutcomeTally> perPrayer,
        int competitionPoints, boolean allPrayed) {
}
