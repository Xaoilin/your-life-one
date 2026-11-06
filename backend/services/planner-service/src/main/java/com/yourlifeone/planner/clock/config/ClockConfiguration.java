package com.yourlifeone.planner.clock.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Clock settings and its numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClockProperties.class)
public class ClockConfiguration {
    /** Stopwatches and timers kept, and how many are running. */
    @Bean
    BusinessGauge clockItemsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.clock.items", "Stopwatches and timers by whether they are running",
                () -> jdbc.sql("""
                        SELECT 'stopwatch' AS kind, started_at IS NOT NULL AS running, count(*) AS total
                        FROM clock.stopwatch GROUP BY 2
                        UNION ALL
                        SELECT 'timer', status = 'running', count(*) FROM clock.timer GROUP BY 2""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "kind", rs.getString("kind"),
                                "running", String.valueOf(rs.getBoolean("running"))))
                        .list());
    }
}
