package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TrackingResponse(
        Instant trackingStartedAt, LocalDate activationDate, List<PrayerName> activationPrayers, Instant importedAt) {
}
