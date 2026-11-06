package com.yourlifeone.prayer.api.response;

import java.time.LocalDate;
import java.util.List;

public record RecentDayResponse(LocalDate date, List<RecentPrayerResponse> prayers) {
}
