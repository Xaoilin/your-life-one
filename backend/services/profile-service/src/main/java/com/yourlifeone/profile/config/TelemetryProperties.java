package com.yourlifeone.profile.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operational telemetry from the React app ({@code app.telemetry.*}).
 *
 * @param maxEventAge oldest event accepted; older batches are refused
 * @param maxClockSkew how far in the future an event's time may be, for device clocks that run fast
 */
@ConfigurationProperties("app.telemetry")
public record TelemetryProperties(Duration maxEventAge, Duration maxClockSkew, Integer deduplicationCapacity) {
    public TelemetryProperties {
        deduplicationCapacity = deduplicationCapacity == null ? 20000 : deduplicationCapacity;
        if (deduplicationCapacity < 1 || deduplicationCapacity > 100000) {
            throw new IllegalArgumentException("Telemetry deduplication capacity must be between 1 and 100000");
        }
        maxEventAge = maxEventAge == null ? Duration.ofHours(1) : maxEventAge;
        maxClockSkew = maxClockSkew == null ? Duration.ofMinutes(1) : maxClockSkew;
    }
}
