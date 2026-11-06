package com.yourlifeone.profile.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * One operational event as the React app sends it. Every text field is an allowlisted value, so
 * nothing free-form (and no account content) can become a metric tag.
 */
public record OperationalEventRequest(
        @NotNull @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}") String id,
        @NotNull @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        String correlationId,
        @NotNull @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z") String occurredAt,
        @NotNull @Pattern(regexp = "\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}") String release,
        @NotNull @Pattern(regexp = "auth|database|realtime|browser|release|calendar|github|assistant|employment"
                + "|finance|equity|prayer|profile|life|knowledge|planner") String domain,
        @NotNull @Pattern(regexp = "session|refresh|read|write|version|subscription|heartbeat|connectivity"
                + "|visibility|manifest|reload|request|recovery|render|exception|unhandled_rejection"
                + "|lcp|inp|cls|fcp|ttfb|long_task|navigation|collector|delivery") String operation,
        @NotNull @Pattern(regexp = "ok|failed|pending|changed|recovered") String outcome,
        @NotNull @Pattern(regexp = "ok|network|timeout|unauthorized|forbidden|rate_limited|server_error"
                + "|invalid_response|offline|online|hidden|visible|signed_in|signed_out|token_refreshed"
                + "|initial_session|subscribing|subscribed|closed|channel_error|heartbeat_sent|heartbeat_ok"
                + "|heartbeat_timeout|heartbeat_error|release_available|reload_suppressed|client_update_required"
                + "|circuit_open|paused|unknown|javascript_error|resource_error|react_render_error|telemetry_dropped") String reason,
        @NotNull @Min(0) @Max(100) Integer attempt,
        @NotNull @Min(0) @Max(86_400_000) Long durationMs,
        @Min(0) @Max(86_400_000) Long recoveryMs,
        @NotNull @Pattern(regexp = "fresh|stale|unknown") String freshness,
        @DecimalMin("0") @DecimalMax("86400000") Double measurement) {
}
