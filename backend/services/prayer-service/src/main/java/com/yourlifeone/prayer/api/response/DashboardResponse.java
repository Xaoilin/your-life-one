package com.yourlifeone.prayer.api.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record DashboardResponse(
        LocalDate today,
        PreferencesResponse preferences,
        ScheduleResponse schedule,
        TrackingResponse tracking,
        List<OutcomeResponse> todayOutcomes,
        List<RecentDayResponse> recentDays,
        StatsResponse monthStats) {
}
