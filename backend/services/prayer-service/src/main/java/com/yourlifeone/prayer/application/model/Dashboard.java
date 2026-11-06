package com.yourlifeone.prayer.application.model;

import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.OutcomeStats;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.ScheduleDay;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/** Everything the landing page shows about prayer. */
@Builder
public record Dashboard(
        LocalDate today,
        PrayerProfile profile,
        ScheduleDay schedule,
        List<Outcome> todayOutcomes,
        List<RecentDay> recentDays,
        OutcomeStats monthStats) {
}
