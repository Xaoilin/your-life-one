package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Google's event start or end: {@code date} for all-day events, {@code dateTime} for timed ones. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleEventTime(String date, String dateTime, String timeZone) {
}
