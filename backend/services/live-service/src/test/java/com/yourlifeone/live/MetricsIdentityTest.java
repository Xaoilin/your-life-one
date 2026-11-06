package com.yourlifeone.live;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** Two live gateways must remain distinct all the way through the actual OTLP HTTP export. */
class MetricsIdentityTest {
    @Test
    void exportsDistinctInstanceTagsForBothGateways() throws Exception {
        var collector = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var instances = ConcurrentHashMap.<String>newKeySet();
        var received = new CountDownLatch(2);
        collect(collector, instances, received);
        collector.start();
        try {
            var context = new ApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer())
                    .withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
                            OtlpMetricsExportAutoConfiguration.class))
                    .withPropertyValues("management.otlp.metrics.export.enabled=true",
                            "management.otlp.metrics.export.step=100ms",
                            "management.otlp.metrics.export.headers.Authorization=",
                            "management.otlp.metrics.export.url=http://127.0.0.1:"
                                    + collector.getAddress().getPort() + "/v1/metrics");
            context.withPropertyValues("APP_SERVICE_INSTANCE_ID=live-prayer").run(prayer ->
                    context.withPropertyValues("APP_SERVICE_INSTANCE_ID=live-calendar").run(calendar -> {
                        prayer.getBean(OtlpMeterRegistry.class).counter("yourlife.live.identity").increment();
                        calendar.getBean(OtlpMeterRegistry.class).counter("yourlife.live.identity").increment();
                        assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
                        assertThat(instances).containsExactlyInAnyOrder("live-prayer", "live-calendar");
                    }));
        } finally {
            collector.stop(0);
        }
    }

    private static void collect(HttpServer collector, Set<String> instances, CountDownLatch received) {
        collector.createContext("/v1/metrics", exchange -> {
            try (var input = "gzip".equals(exchange.getRequestHeaders().getFirst("Content-Encoding"))
                    ? new GZIPInputStream(exchange.getRequestBody()) : exchange.getRequestBody()) {
                var body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                if (body.contains("yourlife.live.identity")) {
                    for (var instance : Set.of("live-prayer", "live-calendar")) {
                        if (body.contains(instance) && instances.add(instance)) received.countDown();
                    }
                }
                exchange.sendResponseHeaders(200, -1);
            } finally {
                exchange.close();
            }
        });
    }
}
