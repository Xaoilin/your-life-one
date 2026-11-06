package com.yourlifeone.commonservice.metrics;

import com.yourlifeone.commonservice.changes.ChangeEvent;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionalEventListener;

/** Refreshes every {@link BusinessGauge} from the database; the OTLP registry pushes them to Grafana. */
public class BusinessGaugePublisher {
    private static final Logger logger = LoggerFactory.getLogger(BusinessGaugePublisher.class);

    private final List<Published> gauges;
    private final Clock clock;
    private final long refreshNanos;
    private final Counter reads;
    private final AtomicLong changes = new AtomicLong();
    private final AtomicLong lastSuccessMillis = new AtomicLong();
    private long publishedChange = -1;
    private long lastRefreshNanos;

    private record Published(BusinessGauge gauge, MultiGauge meter) {
    }

    public BusinessGaugePublisher(List<BusinessGauge> gauges, MeterRegistry meters) {
        this(gauges, meters, new BusinessMetricsProperties(null));
    }

    public BusinessGaugePublisher(List<BusinessGauge> gauges, MeterRegistry meters,
            BusinessMetricsProperties properties) {
        this.clock = meters.config().clock();
        this.refreshNanos = properties.refresh().toNanos();
        this.reads = Counter.builder("yourlife.business.metrics.reads")
                .description("Business gauge database read attempts")
                .register(meters);
        Gauge.builder("yourlife.business.metrics.last.success.timestamp", lastSuccessMillis,
                        value -> value.get() / 1000.0)
                .description("Unix time of the last fully successful business gauge refresh; zero until ready")
                .register(meters);
        this.gauges = gauges.stream()
                .map(gauge -> new Published(gauge,
                        MultiGauge.builder(gauge.name()).description(gauge.description()).register(meters)))
                .toList();
    }

    /** Coalesces saved changes; a transaction that rolls back cannot dirty the published snapshot. */
    @TransactionalEventListener(fallbackExecution = true)
    public void changed(ChangeEvent event) {
        invalidate();
    }

    /** Also available to service-owned importers that mutate data without a change event. */
    public void invalidate() {
        changes.incrementAndGet();
    }

    /** Quiet ticks reuse the snapshot. Failed reads retain values and retry on the next tick. */
    @Scheduled(initialDelayString = "${app.business-metrics.initial-delay:PT30S}",
            fixedDelayString = "${app.business-metrics.check-interval:PT1M}")
    public synchronized void refresh() {
        var change = changes.get();
        if (publishedChange == change && clock.monotonicTime() - lastRefreshNanos < refreshNanos) return;
        var successful = true;
        for (var published : gauges) {
            try {
                reads.increment();
                List<MultiGauge.Row<?>> rows = published.gauge().rows().get().stream()
                        .<MultiGauge.Row<?>>map(row -> MultiGauge.Row.of(row.tags(), row.value()))
                        .toList();
                published.meter().register(rows, true);
            } catch (RuntimeException exception) {
                successful = false;
                logger.warn("Business gauge refresh failed: name={} exceptionType={}", published.gauge().name(),
                        exception.getClass().getSimpleName());
            }
        }
        if (successful) {
            publishedChange = change;
            lastRefreshNanos = clock.monotonicTime();
            lastSuccessMillis.set(clock.wallTime());
        }
    }
}
