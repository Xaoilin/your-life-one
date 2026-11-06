package com.yourlifeone.profile.activity.infrastructure.persistence;

import com.yourlifeone.profile.activity.domain.ProductUsageEvent;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores product-usage events. Plain SQL rather than a Spring Data repository because a repeated event
 * must be skipped, not fail the batch: {@code INSERT ... ON CONFLICT DO NOTHING} does that atomically,
 * even for the same event arriving twice at once.
 */
@Repository
@RequiredArgsConstructor
public class ProductUsageEventRepository {
    private final JdbcClient jdbc;
    private final JsonMapper json;

    /** @return true when stored; false when the user already has this event, or one at its place in the session */
    public boolean insertIfAbsent(UUID userId, ProductUsageEvent event) {
        return jdbc.sql("""
                        INSERT INTO activity.product_usage_event (
                            user_id, event_id, schema_version, session_id, sequence, event_kind, occurred_at, surface,
                            feature, action, outcome, duration_ms, error_code, target, release_version, device_class,
                            input_kind, online, reduced_motion, metadata)
                        VALUES (:userId, :eventId, :schemaVersion, :sessionId, :sequence, :kind, :occurredAt, :surface,
                                :feature, :action, :outcome, :durationMs, :errorCode, :target, :releaseVersion,
                                :deviceClass, :inputKind, :online, :reducedMotion, CAST(:metadata AS jsonb))
                        ON CONFLICT DO NOTHING""")
                .param("userId", userId)
                .param("eventId", event.eventId())
                .param("schemaVersion", event.schemaVersion())
                .param("sessionId", event.sessionId())
                .param("sequence", event.sequence())
                .param("kind", event.kind())
                .param("occurredAt", event.occurredAt().atOffset(ZoneOffset.UTC))
                .param("surface", event.surface())
                .param("feature", event.feature())
                .param("action", event.action())
                .param("outcome", event.outcome())
                .param("durationMs", event.durationMs())
                .param("errorCode", event.errorCode())
                .param("target", event.target())
                .param("releaseVersion", event.releaseVersion())
                .param("deviceClass", event.deviceClass())
                .param("inputKind", event.inputKind())
                .param("online", event.online())
                .param("reducedMotion", event.reducedMotion())
                .param("metadata", json.writeValueAsString(event.metadata() == null ? Map.of() : event.metadata()))
                .update() == 1;
    }
}
