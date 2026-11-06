package com.yourlifeone.planner.tasks.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** How a habit repeats: {@code daily}, {@code weekdays} or {@code weekly}. */
public record RecurrenceRequest(@NotBlank @Size(max = 32) String frequency) {
}
