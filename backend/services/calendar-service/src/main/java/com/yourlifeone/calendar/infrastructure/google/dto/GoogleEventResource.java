package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A Google Calendar event as the API returns it; unknown fields are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleEventResource(
        String id, String status, String summary, String description, String location,
        GoogleEventTime start, GoogleEventTime end) {
}
