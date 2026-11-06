package com.yourlifeone.profile.activity.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Product-usage settings and the numbers the Grafana business dashboard shows for them. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ActivityIngestProperties.class, ActivityInsightsProperties.class})
public class ActivityConfiguration {
    /** Tag for events that name no surface (application-wide events). */
    static final String NO_SURFACE = "none";

    /** Which parts of the app are used: events in the last 7 days, by surface (a configured, bounded set). */
    @Bean
    BusinessGauge activityEventsThisWeekGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.activity.events.last7days", "Product-usage events in the last 7 days",
                () -> jdbc.sql("""
                        SELECT coalesce(surface, :noSurface) AS surface, count(*) AS total
                        FROM activity.product_usage_event
                        WHERE occurred_at > now() - interval '7 days' AND occurred_at <= now()
                        GROUP BY 1""")
                        .param("noSurface", NO_SURFACE)
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "surface", rs.getString("surface")))
                        .list());
    }

    /** How often the app is opened: distinct sessions with an event in the last 7 days. */
    @Bean
    BusinessGauge activitySessionsThisWeekGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.activity.sessions.last7days", "App sessions in the last 7 days",
                () -> jdbc.sql("""
                        SELECT count(DISTINCT session_id) AS total
                        FROM activity.product_usage_event
                        WHERE occurred_at > now() - interval '7 days' AND occurred_at <= now()""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total")))
                        .list());
    }
}
