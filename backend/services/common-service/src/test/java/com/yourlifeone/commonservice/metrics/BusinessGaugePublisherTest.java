package com.yourlifeone.commonservice.metrics;

import com.yourlifeone.commonservice.changes.ChangeEvent;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BusinessGaugePublisherTest {
    private final MockClock clock = new MockClock();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
    private final List<BusinessGauge.Row> rows = new ArrayList<>();
    private boolean failing;
    private final BusinessGaugePublisher publisher = new BusinessGaugePublisher(List.of(
            new BusinessGauge("yourlife.test.items", "Items", () -> {
                if (failing) {
                    throw new IllegalStateException("database down");
                }
                return List.copyOf(rows);
            })), meters);

    private Double value(String category) {
        var gauge = meters.find("yourlife.test.items").tag("category", category).gauge();
        return gauge == null ? null : gauge.value();
    }

    @Test
    void publishesTheLatestRowsAndDropsRowsThatAreGone() {
        rows.add(BusinessGauge.Row.of(3, "category", "food"));
        rows.add(BusinessGauge.Row.of(1, "category", "tools"));
        publisher.refresh();

        rows.clear();
        rows.add(BusinessGauge.Row.of(5, "category", "food"));
        publisher.changed(change());
        publisher.refresh();

        assertEquals(5, value("food"));
        assertNull(value("tools"));
    }

    @Test
    void aFailedReadKeepsTheLastValues() {
        rows.add(BusinessGauge.Row.of(2, "category", "food"));
        publisher.refresh();

        failing = true;
        publisher.changed(change());
        publisher.refresh();

        assertEquals(2, value("food"));
    }

    @Test
    void quietTicksDoNotReadTheDatabaseAgain() {
        var reads = new AtomicInteger();
        var quietPublisher = new BusinessGaugePublisher(List.of(
                new BusinessGauge("yourlife.test.quiet", "Quiet", () -> {
                    reads.incrementAndGet();
                    return List.of(BusinessGauge.Row.of(1));
                })), meters);

        quietPublisher.refresh();
        quietPublisher.refresh();

        assertEquals(1, reads.get());
    }

    @Test
    void thirtyQuietMinutesUseEightyPercentFewerGaugeReads() {
        for (var minute = 0; minute < 30; minute++) {
            publisher.refresh();
            clock.add(Duration.ofMinutes(1));
        }

        assertEquals(6, meters.get("yourlife.business.metrics.reads").counter().count());
    }

    @Test
    void savedChangesAreCoalescedAndRefreshOnTheNextTick() {
        publisher.refresh();
        for (var change = 0; change < 20; change++) publisher.changed(change());
        publisher.refresh();
        publisher.refresh();

        assertEquals(2, meters.get("yourlife.business.metrics.reads").counter().count());
    }

    @Test
    void failuresKeepTheirOldFreshnessAndRetryOnTheNextTick() {
        publisher.refresh();
        var lastSuccess = meters.get("yourlife.business.metrics.last.success.timestamp").gauge().value();
        clock.add(Duration.ofMinutes(5));
        failing = true;
        publisher.refresh();
        assertEquals(lastSuccess, meters.get("yourlife.business.metrics.last.success.timestamp").gauge().value());

        failing = false;
        publisher.refresh();
        assertEquals(3, meters.get("yourlife.business.metrics.reads").counter().count());
        assertEquals(clock.wallTime() / 1000.0,
                meters.get("yourlife.business.metrics.last.success.timestamp").gauge().value());
    }

    @Test
    void aChangeDuringAReadIsNotLost() {
        var owner = new AtomicReference<BusinessGaugePublisher>();
        var reads = new AtomicInteger();
        var registry = new SimpleMeterRegistry();
        try {
            var snapshot = new BusinessGaugePublisher(List.of(
                    new BusinessGauge("yourlife.test.concurrent", "Concurrent", () -> {
                        if (reads.incrementAndGet() == 1) owner.get().invalidate();
                        return List.of(BusinessGauge.Row.of(1));
                    })), registry);
            owner.set(snapshot);
            snapshot.refresh();
            snapshot.refresh();
            snapshot.refresh();

            assertEquals(2, reads.get());
        } finally {
            registry.close();
        }
    }

    private static ChangeEvent change() {
        return ChangeEvent.of(UUID.randomUUID(), "test.saved", Instant.EPOCH, null);
    }
}
