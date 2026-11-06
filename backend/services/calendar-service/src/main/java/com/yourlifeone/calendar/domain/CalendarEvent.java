package com.yourlifeone.calendar.domain;

import lombok.Builder;

/** @param googleEventId the mirrored Google event; null for events on local calendars */
@Builder(toBuilder = true)
public record CalendarEvent(
        String id,
        String sourceId,
        String title,
        String description,
        String location,
        EventTiming timing,
        String googleEventId) {
}
