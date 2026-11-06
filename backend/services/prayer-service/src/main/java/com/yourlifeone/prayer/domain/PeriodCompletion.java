package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Conservative early-settlement proof; missing timetables never turn a partial period into a win. */
public final class PeriodCompletion {
    private PeriodCompletion() {
    }

    public static boolean allPrayed(OutcomeStats stats, Map<LocalDate, ScheduleDay> knownDays,
            LocalDate from, LocalDate to, Instant now) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days < 1 || knownDays.size() != days) return false;
        var tally = stats.overall();
        if ((long) tally.onTime() + tally.late() != days * PrayerName.values().length
                || tally.missed() != 0 || tally.pending() != 0 || tally.unclassified() != 0) return false;
        for (var date = from; !date.isAfter(to); date = date.plusDays(1)) {
            var schedule = knownDays.get(date);
            if (schedule == null || date.isAfter(LocalDate.ofInstant(now, schedule.zone()))) return false;
            for (var prayer : PrayerName.values()) {
                if (schedule.window(prayer).isEmpty()) return false;
            }
        }
        return true;
    }
}
