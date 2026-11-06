package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.application.model.RecentDay;
import com.yourlifeone.prayer.application.model.RecentPrayer;
import com.yourlifeone.prayer.domain.DayStatusResolver;
import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.domain.Tracking;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;

/**
 * The landing page's recent-days log: one entry per day, each listing all five prayers with their
 * recorded or inferred status.
 *
 * @param timeline each day's timetable; days before tracking started use {@code fallback}
 */
@Builder
record RecentDaysLog(Tracking tracking, Map<LocalDate, ScheduleDay> timeline, ScheduleDay fallback, Instant now) {

    List<RecentDay> days(LocalDate first, LocalDate last, List<Outcome> outcomes) {
        var outcomesByDay = outcomes.stream().collect(Collectors.groupingBy(Outcome::date,
                Collectors.toMap(Outcome::prayer, Function.identity())));
        var days = new ArrayList<RecentDay>();
        for (var date = first; !date.isAfter(last); date = date.plusDays(1)) {
            days.add(day(date, outcomesByDay.getOrDefault(date, Map.of())));
        }
        return days;
    }

    private RecentDay day(LocalDate date, Map<PrayerName, Outcome> recorded) {
        var schedule = timeline.getOrDefault(date, fallback.onDate(date));
        var prayers = new ArrayList<RecentPrayer>();
        for (var prayer : PrayerName.values()) {
            var outcome = recorded.get(prayer);
            var status = DayStatusResolver.resolve(tracking, outcome, schedule, prayer, now);
            prayers.add(new RecentPrayer(prayer, status, outcome == null ? null : outcome.id()));
        }
        return new RecentDay(date, prayers);
    }
}
