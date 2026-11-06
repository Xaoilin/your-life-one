package com.yourlifeone.planner.tasks.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param timeZone the app's time zone, e.g. {@code Europe/London}: the completion's date is taken in it */
public record CompleteTaskRequest(@NotBlank @Size(max = 64) String timeZone) {
}
