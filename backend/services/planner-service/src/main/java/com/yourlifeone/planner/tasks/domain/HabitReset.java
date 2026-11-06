package com.yourlifeone.planner.tasks.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * The daily reset: a completed habit reopens once on each new day, daily habits by the app's date and
 * prayer habits by the prayer timetable's date. A habit already reset or completed that day is left alone,
 * so running the reset again never reopens anything twice; weekday habits do not reopen at weekends.
 */
public final class HabitReset {
    private static final String WEEKDAYS = "weekdays";

    private HabitReset() {
    }

    /** The day the task reopens on, or null when it stays as it is. */
    public static LocalDate reopensOn(Task task, LocalDate appDate, LocalDate prayerDate) {
        if (!task.isHabit() || task.recurrence() == null || !task.completed()) return null;
        var date = task.isPrayer() ? prayerDate : appDate;
        if (date == null) return null;
        if (task.lastReset() != null && !task.lastReset().isBefore(date)) return null;
        if (WEEKDAYS.equals(task.recurrence()) && isWeekend(date)) return null;
        return date;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
