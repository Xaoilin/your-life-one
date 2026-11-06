package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Part of Google's calendarList response; unknown fields are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleCalendarListEntry(
        String id, String summary, String summaryOverride, String backgroundColor, Boolean primary,
        String accessRole, Boolean selected) {
}
