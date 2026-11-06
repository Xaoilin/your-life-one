package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.DayStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import java.util.UUID;

public record RecentPrayerResponse(PrayerName prayer, DayStatus status, UUID outcomeId) {
}
