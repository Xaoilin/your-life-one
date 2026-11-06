package com.yourlifeone.prayer.api.request;

import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.PrayerName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * One pillar's momentum reminder preference.
 *
 * @param afterPrayers the prayers whose start prompts this pillar, e.g. {@code ["Dhuhr", "Maghrib"]}
 * @param completedOn the latest date whose Level 1 is complete; null if none
 */
public record MomentumReminderRequest(
        @NotNull Pillar pillar,
        @NotNull Boolean enabled,
        @NotNull @Size(max = 5) List<@NotNull PrayerName> afterPrayers,
        LocalDate completedOn) {
}
