package com.yourlifeone.commonservice.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/** Hard ceilings prevent accidental label growth from exhausting a service or its monitoring budget. */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class ObservabilityAutoConfiguration {
    @Bean
    MeterFilter boundedMeterCount() {
        return MeterFilter.maximumAllowableMetrics(6000);
    }

    @Bean
    MeterFilter boundedHttpRoutes() {
        return MeterFilter.maximumAllowableTags("http", "uri", 150, MeterFilter.deny());
    }

    @Bean
    MeterFilter boundedCalendarNames() {
        return MeterFilter.maximumAllowableTags("yourlife", "calendar", 100, MeterFilter.deny());
    }

    @Bean
    MeterFilter boundedCategories() {
        return MeterFilter.maximumAllowableTags("yourlife", "category", 100, MeterFilter.deny());
    }
}
