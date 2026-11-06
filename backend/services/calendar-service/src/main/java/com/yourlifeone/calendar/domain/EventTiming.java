package com.yourlifeone.calendar.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * When an event happens. A timed event is a pair of instants. An all-day event covers whole calendar
 * dates, which it keeps in every time zone: it is stored as UTC midnight of its first day and of the
 * day after its last day, the same exclusive end Google uses.
 */
public record EventTiming(boolean allDay, Instant startsAt, Instant endsAt) {
    public EventTiming {
        if (startsAt == null || endsAt == null) {
            throw new InvalidEventTimingException("An event needs a start and an end.");
        }
        if (endsAt.isBefore(startsAt)) {
            throw new InvalidEventTimingException("An event cannot end before it starts.");
        }
    }

    /** @param endDate the last day of the event (inclusive) */
    public static EventTiming allDay(LocalDate startDate, LocalDate endDate) {
        return new EventTiming(true, midnightUtc(startDate), midnightUtc(endDate.plusDays(1)));
    }

    public static EventTiming timed(Instant startsAt, Instant endsAt) {
        return new EventTiming(false, startsAt, endsAt);
    }

    /**
     * Reads the app's representation: ISO-8601 instants (with an offset) for timed events, and
     * inclusive {@code YYYY-MM-DD} dates for all-day events.
     */
    public static EventTiming parse(boolean allDay, String start, String end) {
        try {
            return allDay
                    ? allDay(LocalDate.parse(start), LocalDate.parse(end))
                    : timed(OffsetDateTime.parse(start).toInstant(), OffsetDateTime.parse(end).toInstant());
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new InvalidEventTimingException(allDay
                    ? "All-day events need start and end dates as YYYY-MM-DD."
                    : "Timed events need start and end as ISO-8601 date-times with an offset.");
        }
    }

    public LocalDate startDate() {
        return LocalDate.ofInstant(startsAt, ZoneOffset.UTC);
    }

    /** The last day of an all-day event. */
    public LocalDate endDate() {
        return exclusiveEndDate().minusDays(1);
    }

    /** The day after the last day, as Google expects. */
    public LocalDate exclusiveEndDate() {
        return LocalDate.ofInstant(endsAt, ZoneOffset.UTC);
    }

    /** The app's start value: an ISO instant, or the first date of an all-day event. */
    public String startValue() {
        return allDay ? startDate().toString() : startsAt.toString();
    }

    /** The app's end value: an ISO instant, or the last (inclusive) date of an all-day event. */
    public String endValue() {
        return allDay ? endDate().toString() : endsAt.toString();
    }

    private static Instant midnightUtc(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
