package com.yourlifeone.planner.clock.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * A timer's state; the ID is in the path. A new timer without a label is named after the next number.
 *
 * @param endsAt when a running timer ends, in epoch milliseconds; null unless running
 */
public record TimerRequest(
        @Size(max = 100) String label,
        @Min(1) long durationMs,
        @Min(0) long remainingMs,
        @Min(0) Long endsAt,
        @NotBlank @Size(max = 32) String status,
        @NotBlank @Size(max = 32) String sound,
        boolean alerting,
        Instant completedAt) {
}
