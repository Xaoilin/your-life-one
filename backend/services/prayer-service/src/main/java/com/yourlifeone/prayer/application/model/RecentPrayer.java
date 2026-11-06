package com.yourlifeone.prayer.application.model;

import com.yourlifeone.prayer.domain.DayStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import java.util.UUID;

/** @param outcomeId the recorded outcome, or null when the status is inferred */
public record RecentPrayer(PrayerName prayer, DayStatus status, UUID outcomeId) {
}
