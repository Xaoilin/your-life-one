package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.domain.OutcomeStats;
import com.yourlifeone.prayer.domain.OutcomeStatsCalculator;
import com.yourlifeone.prayer.domain.PeriodCompletion;
import com.yourlifeone.prayer.domain.ScheduleDay;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Outcome statistics for any date range. */
@Service
@RequiredArgsConstructor
public class StatsService {
    private final PrayerProfileService profiles;
    private final UserScheduleService userSchedules;
    private final OutcomeService outcomes;
    private final Clock clock;

    /** Uses each day's recorded timetable, falling back to the user's latest one. */
    public OutcomeStats stats(UUID userId, LocalDate from, LocalDate to) {
        var now = clock.instant();
        var tracking = profiles.getOrCreate(userId).tracking();
        var latest = userSchedules.latest(userId);
        Map<LocalDate, ScheduleDay> knownDays = latest.map(day -> {
            var today = LocalDate.ofInstant(now, day.zone());
            var last = to.isBefore(today) ? to : today;
            return userSchedules.knownDays(userId, from, last);
        }).orElse(Map.of());
        var scheduleDays = latest.map(day -> {
            var today = LocalDate.ofInstant(now, day.zone());
            var last = to.isBefore(today) ? to : today;
            return List.copyOf(ScheduleTimeline.build(tracking, knownDays, day, from, last).values());
        }).orElse(List.of());
        var result = OutcomeStatsCalculator.calculate(tracking, outcomes.list(userId, from, to), scheduleDays, now);
        return result.toBuilder().allPrayed(PeriodCompletion.allPrayed(result, knownDays, from, to, now)).build();
    }
}
