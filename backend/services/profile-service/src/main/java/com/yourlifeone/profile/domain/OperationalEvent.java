package com.yourlifeone.profile.domain;

import lombok.Builder;

/**
 * One operational event from the React app: normalized metadata only, never account content.
 *
 * @param occurredAt ISO-8601 UTC time on the reporting device
 * @param recoveryMs how long recovery took; null when the event is not a recovery
 */
@Builder
public record OperationalEvent(
        String id,
        String occurredAt,
        String domain,
        String operation,
        String outcome,
        String reason,
        long durationMs,
        Long recoveryMs,
        Double measurement) {
}
