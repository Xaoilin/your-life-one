package com.yourlifeone.profile.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.TelemetryProperties;
import com.yourlifeone.profile.domain.OperationalEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Turns the React app's operational events into metrics, which reach Grafana Cloud with the rest.
 * Only allowlisted, validated fields become tags, so no account content or unbounded value is recorded.
 */
@Service
public class OperationalEventRecorder {
    static final String EVENTS = "yourlife.client.events";
    static final String DURATION = "yourlife.client.event.duration";
    static final String RECOVERY = "yourlife.client.recovery.duration";

    private final MeterRegistry registry;
    private final TelemetryProperties properties;
    private final Clock clock;
    private final Cache<String, Boolean> received;
    private static final Set<String> MEASUREMENTS = Set.of("lcp", "inp", "cls", "fcp", "ttfb", "long_task", "navigation", "collector");

    public OperationalEventRecorder(MeterRegistry registry, TelemetryProperties properties, Clock clock) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
        this.received = Caffeine.newBuilder().maximumSize(properties.deduplicationCapacity())
                .expireAfterWrite(properties.maxEventAge()).build();
    }

    /** Records every event, or none when any is outside the accepted time window. */
    public int record(List<OperationalEvent> events) {
        events.forEach(this::requireRecent);
        events.forEach(this::recordOne);
        return events.size();
    }

    private void requireRecent(OperationalEvent event) {
        if (event.measurement() != null && (!Double.isFinite(event.measurement()) || event.measurement() < 0
                || event.measurement() > 86400000 || !MEASUREMENTS.contains(event.operation()))) {
            throw ApiException.badRequest("invalid_batch", "The measurement is not supported.");
        }
        var now = clock.instant();
        var occurredAt = parse(event.occurredAt());
        if (occurredAt.isBefore(now.minus(properties.maxEventAge()))
                || occurredAt.isAfter(now.plus(properties.maxClockSkew()))) {
            throw ApiException.badRequest("invalid_batch", "An event is too old or in the future.");
        }
    }

    private static Instant parse(String occurredAt) {
        try {
            return Instant.parse(occurredAt);
        } catch (DateTimeParseException exception) {
            throw ApiException.badRequest("invalid_batch", "occurredAt must be an ISO-8601 UTC time.");
        }
    }

    private void recordOne(OperationalEvent event) {
        if (event.id() != null && received.asMap().putIfAbsent(event.id(), Boolean.TRUE) != null) return;
        if (event.measurement() != null) {
            if (event.operation().equals("collector")) {
                registry.counter("yourlife.client.telemetry.dropped").increment(event.measurement());
            } else {
                DistributionSummary.builder("yourlife.client.web." + event.operation())
                        .baseUnit(event.operation().equals("cls") ? "ratio" : "milliseconds")
                        .serviceLevelObjectives(event.operation().equals("cls") ? new double[] {0.1, 0.25, 1}
                                : new double[] {100, 300, 1000, 2500, 4000, 10000})
                        .register(registry).record(event.measurement());
            }
        }
        Counter.builder(EVENTS)
                .description("Operational events reported by the Your Life One web app")
                .tag("domain", event.domain())
                .tag("operation", event.operation())
                .tag("outcome", event.outcome())
                .tag("reason", event.reason())
                .register(registry)
                .increment();
        DistributionSummary.builder(DURATION)
                .description("How long the reported web app operation took")
                .baseUnit("milliseconds")
                .tag("domain", event.domain())
                .tag("operation", event.operation())
                .tag("outcome", event.outcome())
                .register(registry)
                .record(event.durationMs());
        if (event.recoveryMs() != null) {
            DistributionSummary.builder(RECOVERY)
                    .description("How long the web app took to recover from a failure")
                    .baseUnit("milliseconds")
                    .tag("domain", event.domain())
                    .tag("operation", event.operation())
                    .register(registry)
                    .record(event.recoveryMs());
        }
    }
}
