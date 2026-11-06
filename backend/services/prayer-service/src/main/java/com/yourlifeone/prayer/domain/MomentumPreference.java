package com.yourlifeone.prayer.domain;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * When a Daily Momentum pillar may prompt: after the start of each of these prayers, until its Level 1 is
 * complete for the day.
 *
 * @param afterPrayers the prayers whose start is an anchor for this pillar
 * @param completedOn the latest date whose Level 1 is complete, as the app last reported; null if none
 */
@Builder(toBuilder = true)
public record MomentumPreference(Pillar pillar, boolean enabled, List<PrayerName> afterPrayers, LocalDate completedOn) {
    public MomentumPreference {
        afterPrayers = afterPrayers == null ? List.of() : List.copyOf(afterPrayers);
    }

    /** True when the pillar's Level 1 is already complete on that date. */
    public boolean completeOn(LocalDate date) {
        return completedOn != null && !completedOn.isBefore(date);
    }
}
