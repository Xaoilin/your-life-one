package com.yourlifeone.planner.clock.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A countdown timer.
 *
 * @param endsAt when a running timer ends, in epoch milliseconds; null unless running
 * @param status {@code idle}, {@code running} or {@code completed}
 * @param alerting its alarm is sounding until the user acknowledges it
 */
@Builder(toBuilder = true)
public record CountdownTimer(
        String id,
        String label,
        long durationMs,
        long remainingMs,
        Long endsAt,
        String status,
        String sound,
        boolean alerting,
        Instant completedAt) {
}
