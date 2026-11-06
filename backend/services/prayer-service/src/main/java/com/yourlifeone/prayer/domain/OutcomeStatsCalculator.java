package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculates outcome statistics the same way the React app always has.
 *
 * <p>Every scheduled day counts all five prayers. A recorded outcome wins; otherwise a tracked
 * prayer counts as an inferred miss once its window has closed, or as pending before that.
 * Outcomes on days without a timetable still count, but a missing timetable never invents a miss.
 * Outcomes recorded before tracking started are ignored, except legacy unclassified ones.
 */
public final class OutcomeStatsCalculator {
    private final Tracking tracking;
    private final Instant now;
    private final OutcomeCounter overall = new OutcomeCounter();
    private final Map<PrayerName, OutcomeCounter> perPrayer = new EnumMap<>(PrayerName.class);
    private int competitionPoints;
    private final Set<LocalDate> trackedDates = new HashSet<>();

    private OutcomeStatsCalculator(Tracking tracking, Instant now) {
        this.tracking = tracking;
        this.now = now;
        for (var prayer : PrayerName.values()) perPrayer.put(prayer, new OutcomeCounter());
    }

    public static OutcomeStats calculate(
            Tracking tracking, Collection<Outcome> outcomes, List<ScheduleDay> scheduleDays, Instant now) {
        var calculator = new OutcomeStatsCalculator(tracking, now);
        var unmatchedOutcomes = calculator.countScheduledDays(scheduleDays, indexByDayAndPrayer(outcomes));
        calculator.countOutcomesWithoutSchedule(unmatchedOutcomes);
        return calculator.result();
    }

    /** Counts every prayer on every scheduled day; returns the outcomes no scheduled day covered. */
    private Collection<Outcome> countScheduledDays(List<ScheduleDay> scheduleDays, Map<OutcomeKey, Outcome> outcomes) {
        var unmatched = new HashMap<>(outcomes);
        for (var day : scheduleDays) {
            for (var prayer : PrayerName.values()) {
                var outcome = unmatched.remove(new OutcomeKey(day.date(), prayer));
                countScheduledPrayer(day, prayer, outcome);
            }
        }
        return unmatched.values();
    }

    private void countScheduledPrayer(ScheduleDay day, PrayerName prayer, Outcome outcome) {
        if (outcome != null && isCountable(outcome)) {
            countOutcome(outcome);
            return;
        }
        if (day.window(prayer).isEmpty() || !tracking.isTracked(day, prayer, now)) return;
        var windowClosed = DayStatusResolver.hasWindowClosed(day, prayer, now);
        count(day.date(), prayer, windowClosed ? DayStatus.MISSED : DayStatus.PENDING, windowClosed);
    }

    private void countOutcomesWithoutSchedule(Collection<Outcome> outcomes) {
        for (var outcome : outcomes) {
            if (isCountable(outcome)) countOutcome(outcome);
        }
    }

    /** Legacy unclassified outcomes always count; others only if recorded after tracking started. */
    private boolean isCountable(Outcome outcome) {
        return outcome.status() == OutcomeStatus.UNCLASSIFIED
                || !outcome.recordedAt().isBefore(tracking.trackingStartedAt());
    }

    private void countOutcome(Outcome outcome) {
        count(outcome.date(), outcome.prayer(), DayStatus.of(outcome.status()), false);
        competitionPoints += CompetitionPoints.earned(outcome);
    }

    private void count(LocalDate date, PrayerName prayer, DayStatus status, boolean inferred) {
        overall.count(status, inferred);
        perPrayer.get(prayer).count(status, inferred);
        trackedDates.add(date);
    }

    private OutcomeStats result() {
        var tallies = new EnumMap<PrayerName, OutcomeTally>(PrayerName.class);
        perPrayer.forEach((prayer, counter) -> tallies.put(prayer, counter.tally()));
        return OutcomeStats.builder().overall(overall.tally()).trackedDays(trackedDates.size())
                .perPrayer(tallies).competitionPoints(competitionPoints).build();
    }

    private static Map<OutcomeKey, Outcome> indexByDayAndPrayer(Collection<Outcome> outcomes) {
        var index = new HashMap<OutcomeKey, Outcome>();
        for (var outcome : outcomes) index.put(new OutcomeKey(outcome.date(), outcome.prayer()), outcome);
        return index;
    }

    private record OutcomeKey(LocalDate date, PrayerName prayer) {
    }
}
