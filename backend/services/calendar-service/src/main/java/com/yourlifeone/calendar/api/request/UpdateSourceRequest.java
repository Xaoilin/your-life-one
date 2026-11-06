package com.yourlifeone.calendar.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Only the fields present change; Google calendars accept colour and visibility only. */
public record UpdateSourceRequest(
        @Size(min = 1, max = 200) @Pattern(regexp = ".*\\S.*") String name,
        @Size(min = 1, max = 32) String color,
        Boolean visible,
        @Size(min = 1, max = 256) String accountId) {
}
