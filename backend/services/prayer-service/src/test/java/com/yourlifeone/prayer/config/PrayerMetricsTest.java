package com.yourlifeone.prayer.config;

import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerMetricsTest {
    @Autowired
    private SchemaMigrator migrator;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private BusinessGaugePublisher gauges;

    @Autowired
    private MeterRegistry meters;

    @Test
    void publishesTheLast30DaysOfPrayersByPrayerAndStatusForGrafana() {
        migrator.ensureMigrated();
        jdbc.sql("""
                INSERT INTO prayer.outcome (id, user_id, prayer_date, prayer_name, status, recorded_at)
                VALUES (?, ?, current_date, 'Fajr', 'on_time', now())""")
                .params(UUID.randomUUID(), UUID.randomUUID()).update();
        var expected = jdbc.sql("""
                SELECT count(*) FROM prayer.outcome
                WHERE prayer_date >= current_date - 30 AND prayer_name = 'Fajr' AND status = 'on_time'""")
                .query(Long.class).single();

        gauges.invalidate();
        gauges.refresh();

        assertEquals(expected.doubleValue(),
                meters.get("yourlife.prayer.outcomes.last30days").tags("prayer", "Fajr", "status", "on_time").gauge().value());
    }

    @Test
    void publishesReminderReceiptsOfTheLast7DaysByKindAndSnooze() {
        migrator.ensureMigrated();
        jdbc.sql("""
                INSERT INTO prayer.notice (user_id, key, kind, prayer_date, prayer_name, fires_at, expires_at, sent_at)
                VALUES (?, 'opportunity:2026-09-25:Fajr', 'prayer-opportunity', current_date, 'Fajr', now(),
                        now() + interval '30 minutes', now())""")
                .param(UUID.randomUUID()).update();
        var expected = jdbc.sql("""
                SELECT count(*) FROM prayer.notice
                WHERE kind = 'prayer-opportunity' AND snooze_count = 0 AND sent_at >= now() - interval '7 days'""")
                .query(Long.class).single();

        gauges.invalidate();
        gauges.refresh();

        assertEquals(expected.doubleValue(), meters.get("yourlife.prayer.reminders.last7days")
                .tags("kind", "prayer-opportunity", "snoozed", "false").gauge().value());
    }
}
