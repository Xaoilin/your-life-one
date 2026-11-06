package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.domain.Tracking;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lays out one timetable per tracked date in a range: the one recorded for the user that day,
 * otherwise the reference timetable (the React app always reused today's timetable this way).
 */
final class ScheduleTimeline {
    private ScheduleTimeline() {
    }

    static Map<LocalDate, ScheduleDay> build(
            Tracking tracking, Map<LocalDate, ScheduleDay> knownDays, ScheduleDay reference, LocalDate from, LocalDate to) {
        var timeline = new TreeMap<LocalDate, ScheduleDay>();
        var trackingStart = tracking.startDate(reference);
        var first = from.isBefore(trackingStart) ? trackingStart : from;
        for (var date = first; !date.isAfter(to); date = date.plusDays(1)) {
            timeline.put(date, knownDays.getOrDefault(date, reference.onDate(date)));
        }
        return timeline;
    }
}
