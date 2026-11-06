package com.yourlifeone.planner.clock.api.response;

import java.time.Instant;

public record TimerResponse(
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
