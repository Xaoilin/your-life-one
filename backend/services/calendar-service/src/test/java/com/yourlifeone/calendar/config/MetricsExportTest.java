package com.yourlifeone.calendar.config;

import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metrics reach Grafana Cloud only if the OTLP registry is created. Spring Boot 4 creates it only
 * when its OpenTelemetry module is on the classpath; without it export silently never starts.
 */
class MetricsExportTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
                    OtlpMetricsExportAutoConfiguration.class));

    @Test
    void createsTheOtlpRegistryWhenExportIsEnabled() {
        context.withPropertyValues("management.otlp.metrics.export.enabled=true",
                        "management.otlp.metrics.export.url=http://localhost:4318/v1/metrics")
                .run(started -> assertThat(started).hasSingleBean(OtlpMeterRegistry.class));
    }

    @Test
    void sendsNothingWhenExportIsDisabled() {
        context.withPropertyValues("management.otlp.metrics.export.enabled=false")
                .run(started -> assertThat(started).doesNotHaveBean(OtlpMeterRegistry.class));
    }
}
