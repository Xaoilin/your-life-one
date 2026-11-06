package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodCompletionTest {
    private final LocalDate day = LocalDate.parse("2026-09-25");
    private final Instant noon = Instant.parse("2026-09-25T12:00:00Z");

    @Test
    void allFiveExplicitCompletionsCanFinishTodayBeforeMidnight() {
        var schedules = Map.of(day, Fixtures.london(day));
        assertThat(PeriodCompletion.allPrayed(stats(5, 0, 0, 0), schedules, day, day, noon)).isTrue();
        assertThat(PeriodCompletion.allPrayed(stats(4, 0, 1, 0), schedules, day, day, noon)).isFalse();
        assertThat(PeriodCompletion.allPrayed(stats(4, 1, 0, 0), schedules, day, day, noon)).isFalse();
        assertThat(PeriodCompletion.allPrayed(stats(4, 0, 0, 1), schedules, day, day, noon)).isFalse();
    }

    @Test
    void aWeekWaitsForItsLastDayAndEveryKnownTimetable() {
        var from = day.minusDays(6);
        var schedules = new HashMap<LocalDate, ScheduleDay>();
        for (var date = from; !date.isAfter(day); date = date.plusDays(1)) schedules.put(date, Fixtures.london(date));
        assertThat(PeriodCompletion.allPrayed(stats(35, 0, 0, 0), schedules, from, day, noon)).isTrue();
        assertThat(PeriodCompletion.allPrayed(stats(35, 0, 0, 0), schedules, from, day, noon.minusSeconds(86400))).isFalse();
        schedules.remove(from);
        assertThat(PeriodCompletion.allPrayed(stats(35, 0, 0, 0), schedules, from, day, noon)).isFalse();
    }

    @Test
    void missingOrUnclassifiedOpportunitiesNeverProveCompletion() {
        assertThat(PeriodCompletion.allPrayed(stats(5, 0, 0, 0), Map.of(), day, day, noon)).isFalse();
        assertThat(PeriodCompletion.allPrayed(stats(4, 0, 0, 1), Map.of(day, Fixtures.london(day)), day, day, noon)).isFalse();
    }

    private OutcomeStats stats(int completed, int missed, int pending, int unclassified) {
        return OutcomeStats.builder().overall(OutcomeTally.builder().onTime(completed).missed(missed)
                .pending(pending).unclassified(unclassified).build()).build();
    }
}
