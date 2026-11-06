package com.yourlifeone.calendar.api.response;

/**
 * @param start ISO instant, or the first date of an all-day event
 * @param end ISO instant, or the last (inclusive) date of an all-day event
 */
public record EventResponse(
        String id, String sourceId, String title, String description, String location, boolean allDay,
        String start, String end, String googleEventId) {
}
