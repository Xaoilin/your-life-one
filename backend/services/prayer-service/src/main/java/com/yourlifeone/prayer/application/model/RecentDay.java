package com.yourlifeone.prayer.application.model;

import java.time.LocalDate;
import java.util.List;

/** One day of the recent-days log, with every prayer's status in order. */
public record RecentDay(LocalDate date, List<RecentPrayer> prayers) {
}
