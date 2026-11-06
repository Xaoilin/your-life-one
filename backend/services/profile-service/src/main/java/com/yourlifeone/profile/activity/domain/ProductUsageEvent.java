package com.yourlifeone.profile.activity.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

/**
 * One content-free product-usage event. Feature and action are stable taxonomy keys, never labels,
 * user text or domain values.
 *
 * @param sequence the event's position within its session, from 1
 * @param surface the app surface, or null for application-wide events
 * @param outcome null when the event reports none
 * @param metadata allowlisted keys with scalar values; null means none
 */
@Builder
public record ProductUsageEvent(
        UUID eventId,
        Integer schemaVersion,
        UUID sessionId,
        Integer sequence,
        String kind,
        Instant occurredAt,
        String surface,
        String feature,
        String action,
        String outcome,
        Integer durationMs,
        String errorCode,
        String target,
        String releaseVersion,
        String deviceClass,
        String inputKind,
        Boolean online,
        Boolean reducedMotion,
        Map<String, Object> metadata) {
}
