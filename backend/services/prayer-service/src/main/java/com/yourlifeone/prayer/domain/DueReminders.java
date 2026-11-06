package com.yourlifeone.prayer.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Which deadline reminders are due now: a prayer not yet recorded whose on-time window closes within the
 * user's reminder lead time. Yesterday's Isha counts too, as its window runs past midnight.
 */
public final class DueReminders {
    private DueReminders() {
    }

    /**
     * @param days the timetables for yesterday and today
     * @param recorded outcomes already recorded, as {@code date + prayer} keys from {@link #key}
     */
    public static List<Reminder> due(Collection<ScheduleDay> days, Set<String> recorded, int reminderMinutes,
            Instant now) {
        var lead = Duration.ofMinutes(reminderMinutes);
        var due = new ArrayList<Reminder>();
        for (var day : days) {
            for (var prayer : PrayerName.values()) {
                if (recorded.contains(key(day, prayer))) continue;
                day.window(prayer)
                        .filter(window -> !now.isBefore(window.deadlineAt().minus(lead)) && now.isBefore(window.deadlineAt()))
                        .ifPresent(window -> due.add(Reminder.builder()
                                .date(day.date())
                                .prayer(prayer)
                                .deadlineAt(window.deadlineAt())
                                .deadline(window.deadline())
                                .zone(day.zone())
                                .reminderMinutes(reminderMinutes)
                                .build()));
            }
        }
        due.sort(Comparator.comparing(Reminder::deadlineAt));
        return due;
    }

    public static String key(ScheduleDay day, PrayerName prayer) {
        return day.date() + "|" + prayer.label();
    }
}
