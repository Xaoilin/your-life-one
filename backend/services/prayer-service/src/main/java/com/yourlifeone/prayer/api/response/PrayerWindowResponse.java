package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Instant;
import lombok.Builder;

@Builder
public record PrayerWindowResponse(PrayerName prayer, Instant startsAt, String deadlineName, Instant deadlineAt) {
}
