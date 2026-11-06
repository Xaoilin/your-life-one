package com.yourlifeone.commonservice.autoconfigure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ObservabilityAutoConfigurationTest {
    @Test
    void boundsAccidentalRouteAndBusinessLabelGrowthWithoutBreakingCallers() {
        var config = new ObservabilityAutoConfiguration();
        var registry = new SimpleMeterRegistry();
        registry.config().meterFilter(config.boundedHttpRoutes()).meterFilter(config.boundedCalendarNames());
        assertDoesNotThrow(() -> {
            for (int index = 0; index < 1000; index++) {
                registry.counter("http.server.requests", "uri", "/synthetic/" + index).increment();
                registry.counter("yourlife.calendar.events", "calendar", "synthetic-" + index).increment();
            }
        });
        assertEquals(150, registry.find("http.server.requests").counters().size());
        assertEquals(100, registry.find("yourlife.calendar.events").counters().size());
    }
}
