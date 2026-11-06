package com.yourlifeone.calendar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Calendar service settings ({@code app.calendar.*}).
 *
 * @param syncPastDays sync mirrors Google events from this many days ago
 * @param syncFutureDays sync mirrors Google events up to this many days ahead
 * @param maxRangeDays the longest date range one events request may ask for
 * @param defaultSourceName name of the calendar a new local account starts with
 * @param defaultSourceColor colour of new calendars that do not bring their own
 * @param googleApiBaseUrl Google Calendar API v3 root
 */
@ConfigurationProperties("app.calendar")
public record CalendarProperties(
        int syncPastDays,
        int syncFutureDays,
        int maxRangeDays,
        String defaultSourceName,
        String defaultSourceColor,
        String googleApiBaseUrl) {
    public CalendarProperties {
        if (syncPastDays < 0 || syncFutureDays < 1) {
            throw new IllegalArgumentException("app.calendar sync window must cover at least one future day");
        }
        if (maxRangeDays < 1) {
            throw new IllegalArgumentException("app.calendar.max-range-days must be positive");
        }
    }
}
