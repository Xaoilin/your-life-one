package com.yourlifeone.calendar.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A local calendar in a local account. */
public record CreateSourceRequest(
        @NotBlank @Size(max = 256) String accountId,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 32) String color,
        Boolean visible) {
}
