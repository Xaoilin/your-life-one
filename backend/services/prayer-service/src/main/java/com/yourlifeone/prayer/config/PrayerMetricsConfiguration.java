package com.yourlifeone.prayer.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Prayer numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
public class PrayerMetricsConfiguration {
    /** How consistently each prayer is kept: recorded outcomes of the last 30 days by prayer and status. */
    @Bean
    BusinessGauge prayerOutcomesGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.prayer.outcomes.last30days", "Prayers recorded in the last 30 days",
                () -> jdbc.sql("""
                        SELECT prayer_name, status, count(*) AS total FROM prayer.outcome
                        WHERE prayer_date >= current_date - 30
                        GROUP BY prayer_name, status""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "prayer", rs.getString("prayer_name"), "status", rs.getString("status")))
                        .list());
    }

    /** How many reminders go out and how often they are snoozed: receipts of the last 7 days by kind. */
    @Bean
    BusinessGauge prayerRemindersGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.prayer.reminders.last7days", "Reminder receipts of the last 7 days",
                () -> jdbc.sql("""
                        SELECT 'deadline' AS kind, snooze_count > 0 AS snoozed, count(*) AS total FROM prayer.reminder
                        WHERE sent_at >= now() - interval '7 days' GROUP BY 1, 2
                        UNION ALL
                        SELECT kind, snooze_count > 0, count(*) FROM prayer.notice
                        WHERE sent_at >= now() - interval '7 days' GROUP BY 1, 2""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "kind", rs.getString("kind"), "snoozed", String.valueOf(rs.getBoolean("snoozed"))))
                        .list());
    }
}
