package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.TelemetryProperties;
import com.yourlifeone.profile.domain.OperationalEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationalEventRecorderTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OperationalEventRecorder recorder = new OperationalEventRecorder(registry,
            new TelemetryProperties(Duration.ofHours(1), Duration.ofMinutes(1), 20000), Clock.fixed(NOW, ZoneOffset.UTC));

    private static OperationalEvent.OperationalEventBuilder event() {
        return OperationalEvent.builder().occurredAt("2026-09-26T11:59:00.000Z").domain("database")
                .operation("read").outcome("failed").reason("timeout").durationMs(1500);
    }

    @Test
    void countsEachEventAndRecordsItsDuration() {
        recorder.record(List.of(event().build(), event().build(), event().outcome("recovered").recoveryMs(4000L).build()));

        var failures = registry.get(OperationalEventRecorder.EVENTS).tags("domain", "database", "operation", "read",
                "outcome", "failed", "reason", "timeout").counter();
        assertEquals(2, failures.count());
        assertEquals(3, registry.get(OperationalEventRecorder.DURATION).summaries().stream()
                .mapToLong(summary -> summary.count()).sum());
        assertEquals(4000, registry.get(OperationalEventRecorder.RECOVERY).summary().totalAmount());
    }

    @Test
    void retriesDoNotInflateMeasurementsOrEventCounts() {
        var measurement = event().id("synthetic-event").domain("browser").operation("lcp")
                .outcome("ok").reason("ok").measurement(2400.5).build();
        assertEquals(1, recorder.record(List.of(measurement)));
        assertEquals(1, recorder.record(List.of(measurement)));
        assertEquals(1, registry.get("yourlife.client.web.lcp").summary().count());
        assertEquals(2400.5, registry.get("yourlife.client.web.lcp").summary().totalAmount());
        assertEquals(1, registry.get(OperationalEventRecorder.EVENTS).counter().count());
    }

    @Test
    void refusesNonFiniteOrUnsupportedMeasurementsBeforeRecordingAnything() {
        for (var value : List.of(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 86400001.0)) {
            assertThrows(ApiException.class, () -> recorder.record(List.of(event().build(),
                    event().operation("lcp").measurement(value).build())));
        }
        assertThrows(ApiException.class, () -> recorder.record(List.of(event().measurement(1.0).build())));
        assertNull(registry.find(OperationalEventRecorder.EVENTS).counter());
    }

    @Test
    void refusesTheWholeBatchWhenAnEventIsOutsideTheWindow() {
        var stale = event().occurredAt("2026-09-26T10:59:59.000Z").build();
        var future = event().occurredAt("2026-09-26T12:01:01.000Z").build();

        for (var outside : List.of(stale, future)) {
            var error = assertThrows(ApiException.class, () -> recorder.record(List.of(event().build(), outside)));
            assertEquals("invalid_batch", error.code());
        }
        assertNull(registry.find(OperationalEventRecorder.EVENTS).counter());
    }
}
