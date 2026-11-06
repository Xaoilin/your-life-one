package com.yourlifeone.planner.clock.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A stopwatch's state; the ID is in the path. A new stopwatch without a label is named after the next number.
 *
 * @param startedAt when the current run started, in epoch milliseconds; null while stopped
 */
public record StopwatchRequest(
        @Size(max = 100) String label,
        @Min(0) long accumulatedMs,
        @Min(0) Long startedAt,
        @NotNull List<@NotNull @Min(0) Long> laps) {
}
