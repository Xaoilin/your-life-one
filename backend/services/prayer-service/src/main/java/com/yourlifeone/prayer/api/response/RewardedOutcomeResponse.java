package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/** A prayer recorded as prayed, whose one-time reward receipt is set. */
@Builder
public record RewardedOutcomeResponse(LocalDate date, PrayerName prayer, Instant recordedAt, String taskId) {
}
