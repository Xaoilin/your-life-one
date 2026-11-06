package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.Dashboard;
import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.OutcomeStatsCalculator;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.ScheduleDay;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the landing page's prayer data. Reading it also records today's timetable for the
 * user, freezes the activation-day eligibility, and stores misses whose windows have closed.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {
    static final int RECENT_DAYS = 7;

    private final PrayerProfileService profiles;
    private final ScheduleService schedules;
    private final UserScheduleService userSchedules;
    private final OutcomeService outcomes;
    private final MissedPrayerRecorder missedPrayers;
    private final Clock clock;

    public Dashboard dashboard(UUID userId, String city, String country) {
        var now = clock.instant();
        var schedule = schedules.today(city, country);
        userSchedules.remember(userId, schedule);
        var profile = withActivationCaptured(userId, profiles.getOrCreate(userId), schedule);
        var tracking = profile.tracking();

        var today = schedule.date();
        var monthStart = today.withDayOfMonth(1);
        var recentStart = today.minusDays(RECENT_DAYS - 1);
        var from = monthStart.isBefore(recentStart) ? monthStart : recentStart;

        var knownDays = userSchedules.knownDays(userId, from, today);
        missedPrayers.recordClosedWindows(userId, tracking, knownDays, today, now);
        var periodOutcomes = outcomes.list(userId, from, today);
        var timeline = ScheduleTimeline.build(tracking, knownDays, schedule, from, today);

        return Dashboard.builder()
                .today(today)
                .profile(profile)
                .schedule(schedule)
                .todayOutcomes(onOrAfter(periodOutcomes, today))
                .recentDays(RecentDaysLog.builder()
                        .tracking(tracking)
                        .timeline(timeline)
                        .fallback(schedule)
                        .now(now)
                        .build()
                        .days(recentStart, today, periodOutcomes))
                .monthStats(OutcomeStatsCalculator.calculate(tracking, onOrAfter(periodOutcomes, monthStart),
                        onOrAfter(timeline, monthStart), now))
                .build();
    }

    private PrayerProfile withActivationCaptured(UUID userId, PrayerProfile profile, ScheduleDay today) {
        var captured = profile.tracking().captureActivation(today);
        if (captured == profile.tracking()) return profile;
        profiles.saveActivation(userId, captured);
        return profile.toBuilder().tracking(captured).build();
    }

    private static List<Outcome> onOrAfter(List<Outcome> outcomes, LocalDate first) {
        return outcomes.stream().filter(outcome -> !outcome.date().isBefore(first)).toList();
    }

    private static List<ScheduleDay> onOrAfter(Map<LocalDate, ScheduleDay> timeline, LocalDate first) {
        return timeline.values().stream().filter(day -> !day.date().isBefore(first)).toList();
    }
}
