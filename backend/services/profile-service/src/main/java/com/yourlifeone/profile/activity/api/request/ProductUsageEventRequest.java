package com.yourlifeone.profile.activity.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

/**
 * One product-usage event, exactly as the React app's {@code ProductUsageEvent} type sends it. Allowed
 * values are checked against configuration ({@code app.profile.activity.ingest.*}).
 *
 * @param surface absent for application-wide events
 * @param metadata allowlisted keys with scalar values; absent means none
 */
@Builder
public record ProductUsageEventRequest(
        @NotNull UUID eventId,
        @NotNull Integer schemaVersion,
        @NotNull UUID sessionId,
        @NotNull Integer sequence,
        @NotNull String kind,
        @NotNull Instant occurredAt,
        String surface,
        @NotNull String feature,
        @NotNull String action,
        String outcome,
        Integer durationMs,
        String errorCode,
        String target,
        @NotNull String releaseVersion,
        @NotNull String deviceClass,
        @NotNull String inputKind,
        @NotNull Boolean online,
        @NotNull Boolean reducedMotion,
        Map<String, Object> metadata) {
}
