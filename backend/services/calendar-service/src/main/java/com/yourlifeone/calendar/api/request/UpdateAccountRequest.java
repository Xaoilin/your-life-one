package com.yourlifeone.calendar.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Only the fields present change. {@code primary: true} makes every other account not primary. */
public record UpdateAccountRequest(
        @Size(min = 1, max = 200) @Pattern(regexp = ".*\\S.*") String name,
        Boolean primary,
        @Min(0) @Max(99) Integer paletteIndex) {
}
