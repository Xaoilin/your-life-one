package com.yourlifeone.profile.activity.api.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Product-usage events as the React app sends them, for request bodies in tests. */
public final class ActivityEventJson {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    private ActivityEventJson(UUID sessionId, int sequence, String kind, String feature, String action) {
        fields.put("eventId", UUID.randomUUID());
        fields.put("schemaVersion", 1);
        fields.put("sessionId", sessionId);
        fields.put("sequence", sequence);
        fields.put("kind", kind);
        fields.put("occurredAt", Instant.now().minus(Duration.ofMinutes(sequence)).truncatedTo(ChronoUnit.MILLIS));
        fields.put("feature", feature);
        fields.put("action", action);
        fields.put("releaseVersion", "0.2.203");
        fields.put("deviceClass", "desktop");
        fields.put("inputKind", "pointer");
        fields.put("online", true);
        fields.put("reducedMotion", false);
        fields.put("metadata", Map.of());
    }

    public static ActivityEventJson event(UUID sessionId, int sequence, String kind, String feature, String action) {
        return new ActivityEventJson(sessionId, sequence, kind, feature, action);
    }

    /**
     * Three sessions now and one path in the previous 30-day window: 10 events in the last 30 days, 2 of
     * them failures (one coded, one without a code or surface).
     */
    public static List<ActivityEventJson> threeSessions() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var third = UUID.randomUUID();
        return List.of(
                event(first, 1, "session", "application", "session_started"),
                event(first, 2, "navigation", "navigation", "viewed").with("surface", "calendar"),
                event(first, 3, "outcome", "navigation", "surface_opened").with("surface", "calendar")
                        .with("outcome", "success").with("durationMs", 120),
                event(first, 4, "error", "calendar", "render_failed").with("surface", "calendar")
                        .with("outcome", "failure").with("errorCode", "calendar_render"),
                event(second, 1, "session", "application", "session_started"),
                event(second, 2, "navigation", "navigation", "viewed").with("surface", "dashboard")
                        .with("metadata", Map.of("previousSurface", "calendar", "retryCount", 1)),
                event(second, 3, "action", "tasks", "created").with("surface", "tasks"),
                event(third, 1, "session", "application", "session_started"),
                event(third, 2, "navigation", "navigation", "viewed").with("surface", "dashboard"),
                event(third, 3, "action", "tasks", "save_failed").with("outcome", "failure"),
                event(UUID.randomUUID(), 1, "action", "tasks", "created").with("surface", "tasks")
                        .ago(Duration.ofDays(40)));
    }

    public static String batch(List<ActivityEventJson> events) {
        return events.stream().map(ActivityEventJson::toJson).collect(Collectors.joining(",", "{\"events\":[", "]}"));
    }

    public ActivityEventJson with(String field, Object value) {
        fields.put(field, value);
        return this;
    }

    public ActivityEventJson ago(Duration age) {
        return with("occurredAt", Instant.now().minus(age).truncatedTo(ChronoUnit.MILLIS));
    }

    public String toJson() {
        return fields.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":" + value(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String value(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> "\"" + entry.getKey() + "\":" + value(entry.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Number || value instanceof Boolean || value == null) return String.valueOf(value);
        return "\"" + value + "\"";
    }
}
