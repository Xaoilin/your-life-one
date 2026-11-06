package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * When outcome tracking began for a user.
 *
 * @param activationDate the date whose eligible prayers were frozen; null until captured
 * @param activationPrayers prayers still open when tracking began on {@code activationDate}
 */
public record Tracking(Instant trackingStartedAt, LocalDate activationDate, List<PrayerName> activationPrayers) {
    public Tracking {
        activationPrayers = activationPrayers == null ? List.of() : List.copyOf(activationPrayers);
    }

    /** The calendar date tracking began, in the timetable's own time zone. */
    public LocalDate startDate(ScheduleDay day) {
        return LocalDate.ofInstant(trackingStartedAt, day.zone());
    }

    /**
     * Whether a scheduled prayer counts toward tracking: never before the start date, always
     * after it, and on the start date only if it was still open when tracking began.
     */
    public boolean isTracked(ScheduleDay day, PrayerName prayer, Instant now) {
        var window = day.window(prayer);
        if (window.isEmpty()) return false;
        var startDate = startDate(day);
        if (day.date().isBefore(startDate)) return false;
        if (day.date().isAfter(startDate)) return true;
        if (startDate.equals(activationDate)) return activationPrayers.contains(prayer);
        boolean stillActivationDay = LocalDate.ofInstant(now, day.zone()).equals(startDate);
        return stillActivationDay && window.get().deadlineAt().isAfter(trackingStartedAt);
    }

    /**
     * Freezes which prayers were still open on the activation day, so later timetable or
     * location changes cannot rewrite it. Needs the activation day's complete timetable.
     */
    public Tracking captureActivation(ScheduleDay day) {
        if (activationDate != null || !day.date().equals(startDate(day))) return this;
        var stillOpen = new ArrayList<PrayerName>();
        for (var prayer : PrayerName.values()) {
            var window = day.window(prayer);
            if (window.isEmpty()) return this;
            if (window.get().deadlineAt().isAfter(trackingStartedAt)) stillOpen.add(prayer);
        }
        return new Tracking(trackingStartedAt, day.date(), stillOpen);
    }
}
