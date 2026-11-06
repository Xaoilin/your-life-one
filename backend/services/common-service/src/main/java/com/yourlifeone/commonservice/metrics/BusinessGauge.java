package com.yourlifeone.commonservice.metrics;

import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.function.Supplier;

/**
 * A business number a service publishes to Grafana, such as prayers on time or upcoming events per
 * calendar. {@link BusinessGaugePublisher} reads the rows again on every refresh; a row missing from
 * the latest read is removed. Tag values must come from a small, bounded set.
 *
 * @param name metric name, {@code yourlife.<domain>.<what>}
 * @param rows reads the current values, one row per tag combination
 */
public record BusinessGauge(String name, String description, Supplier<List<Row>> rows) {
    public record Row(Tags tags, double value) {
        /** @param tags alternating tag keys and values */
        public static Row of(double value, String... tags) {
            return new Row(Tags.of(tags), value);
        }
    }
}
