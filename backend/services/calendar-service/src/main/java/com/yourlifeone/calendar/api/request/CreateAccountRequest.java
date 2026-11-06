package com.yourlifeone.calendar.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A local account; Google accounts are added through the Google consent popup. */
public record CreateAccountRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 320) String email,
        @Min(0) @Max(99) Integer paletteIndex) {
}
