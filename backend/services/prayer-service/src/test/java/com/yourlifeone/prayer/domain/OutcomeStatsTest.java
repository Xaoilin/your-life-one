package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static com.yourlifeone.prayer.domain.Fixtures.london;
import static com.yourlifeone.prayer.domain.Fixtures.outcome;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OutcomeStatsTest {
    private static final Instant STARTED = Instant.parse("2026-09-24T00:00:00Z");
    private static final Tracking TRACKING = new Tracking(STARTED, DAY.minusDays(1), List.of(PrayerName.values()));

    @Test
    void recordedOutcomesWinAndClosedWindowsAreInferredMisses() {
        var now = Instant.parse("2026-09-25T16:00:00Z"); // Dhuhr closed at 15:08Z; Asr still open
        var outcomes = List.of(
                outcome(DAY, PrayerName.FAJR, OutcomeStatus.ON_TIME, now),
                outcome(DAY, PrayerName.ASR, OutcomeStatus.LATE, now));

        var stats = OutcomeStatsCalculator.calculate(TRACKING, outcomes, List.of(london(DAY)), now);

        var overall = stats.overall();
        assertEquals(1, overall.onTime());
        assertEquals(1, overall.late());
        assertEquals(1, overall.missed());
        assertEquals(1, overall.inferredMissed());
        assertEquals(2, overall.pending());
        assertEquals(3, overall.classifiedTotal());
        assertEquals(5, overall.opportunities());
        assertEquals(new OutcomePercentages(34, 33, 33), overall.percentages());
        assertEquals(1, stats.trackedDays());
        assertEquals(1, stats.perPrayer().get(PrayerName.DHUHR).inferredMissed());
    }

    @Test
    void outcomesOutsideTheScheduleStillCountButNeverInventMisses() {
        var now = Instant.parse("2026-09-25T23:59:00Z");
        var outcomes = List.of(
                outcome(DAY.minusDays(1), PrayerName.ISHA, OutcomeStatus.ON_TIME, now),
                outcome(DAY.minusDays(1), PrayerName.FAJR, OutcomeStatus.UNCLASSIFIED, STARTED.minusSeconds(60)),
                outcome(DAY.minusDays(1), PrayerName.DHUHR, OutcomeStatus.LATE, STARTED.minusSeconds(60)));

        var stats = OutcomeStatsCalculator.calculate(TRACKING, outcomes, List.of(), now);

        assertEquals(1, stats.overall().onTime());
        assertEquals(1, stats.overall().unclassified());
        assertEquals(0, stats.overall().late()); // recorded before tracking started
        assertEquals(0, stats.overall().missed());
        assertEquals(1, stats.trackedDays());
    }

    @Test
    void recentDayStatusFollowsTheSameRules() {
        var now = Instant.parse("2026-09-25T16:00:00Z");
        var day = london(DAY);
        var tracking = new Tracking(Instant.parse("2026-09-25T12:00:00Z"), DAY,
                List.of(PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA));

        assertEquals(DayStatus.NOT_TRACKED, DayStatusResolver.resolve(tracking, null, day, PrayerName.FAJR, now));
        assertEquals(DayStatus.MISSED, DayStatusResolver.resolve(tracking, null, day, PrayerName.DHUHR, now));
        assertEquals(DayStatus.PENDING, DayStatusResolver.resolve(tracking, null, day, PrayerName.ASR, now));
        assertEquals(DayStatus.LATE, DayStatusResolver.resolve(tracking,
                outcome(DAY, PrayerName.ASR, OutcomeStatus.LATE, now), day, PrayerName.ASR, now));
        assertEquals(DayStatus.PENDING, DayStatusResolver.resolve(tracking, null, null, PrayerName.ASR, now));
    }

    @Test
    void emptyPeriodsHaveZeroPercentages() {
        var stats = OutcomeStatsCalculator.calculate(TRACKING, List.of(), List.of(), Instant.parse("2026-09-25T00:00:00Z"));

        assertEquals(new OutcomePercentages(0, 0, 0), stats.overall().percentages());
        assertEquals(0, stats.trackedDays());
    }
    @Test
    void competitionPointsUseTheSameEligibilityAndKeepLegacyCompletionCredit() {
        var now = Instant.parse("2026-09-25T16:00:00Z");
        var outcomes = List.of(
                outcome(DAY, PrayerName.FAJR, OutcomeStatus.ON_TIME, now).toBuilder().timing(OutcomeTiming.PROMPT).build(),
                outcome(DAY, PrayerName.DHUHR, OutcomeStatus.ON_TIME, now).toBuilder().timing(OutcomeTiming.WITHIN_WINDOW).build(),
                outcome(DAY, PrayerName.ASR, OutcomeStatus.LATE, now).toBuilder().timing(OutcomeTiming.OUTSIDE_WINDOW).build(),
                outcome(DAY, PrayerName.MAGHRIB, OutcomeStatus.MISSED, now),
                outcome(DAY, PrayerName.ISHA, OutcomeStatus.UNCLASSIFIED, STARTED.minusSeconds(60)),
                outcome(DAY.minusDays(1), PrayerName.FAJR, OutcomeStatus.LATE, now),
                outcome(DAY.minusDays(1), PrayerName.DHUHR, OutcomeStatus.ON_TIME, STARTED.minusSeconds(60)));
        for (var schedule : List.of(List.<ScheduleDay>of(), List.of(london(DAY)))) {
            var stats = OutcomeStatsCalculator.calculate(TRACKING, outcomes, schedule, now);
            assertEquals(34, stats.competitionPoints()); // 12 + 8 + 2 + legacy 12
            assertEquals(5, stats.overall().classifiedTotal());
            assertEquals(1, stats.overall().unclassified());
        }
    }

    @Test
    void inferredMissesAndPendingPrayersAddNoCompetitionPoints() {
        var now = Instant.parse("2026-09-25T16:00:00Z");
        var stats = OutcomeStatsCalculator.calculate(TRACKING, List.of(), List.of(london(DAY)), now);
        assertEquals(0, stats.competitionPoints());
        assertEquals(2, stats.overall().inferredMissed());
        assertEquals(3, stats.overall().pending());
    }

}
