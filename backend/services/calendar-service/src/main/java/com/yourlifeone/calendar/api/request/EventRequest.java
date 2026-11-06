package com.yourlifeone.calendar.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An event as the user sets it.
 *
 * @param start ISO-8601 date-time with offset, or the first date (YYYY-MM-DD) of an all-day event
 * @param end ISO-8601 date-time with offset, or the last date (YYYY-MM-DD, inclusive) of an all-day event
 * @param timeZone IANA zone Google shows a timed event in, normally the app's time zone
 */
public record EventRequest(
        @NotBlank @Size(max = 256) String sourceId,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 8000) String description,
        @Size(max = 500) String location,
        @NotNull Boolean allDay,
        @NotBlank @Size(max = 40) String start,
        @NotBlank @Size(max = 40) String end,
        @Size(max = 64) String timeZone) {
}
