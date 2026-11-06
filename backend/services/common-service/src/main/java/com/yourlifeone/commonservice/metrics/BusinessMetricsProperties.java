package com.yourlifeone.commonservice.metrics;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Maximum age of a quiet service's business numbers; saved changes invalidate them sooner. */
@ConfigurationProperties("app.business-metrics")
public record BusinessMetricsProperties(Duration refresh) {
    private static final Duration DEFAULT_REFRESH = Duration.ofMinutes(5);

    public BusinessMetricsProperties {
        refresh = refresh == null ? DEFAULT_REFRESH : refresh;
        if (refresh.isZero() || refresh.isNegative()) {
            throw new IllegalArgumentException("Business metric refresh must be positive");
        }
    }
}
