package com.yourlifeone.profile.activity.config;

import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.profile.infrastructure.persistence.ProfileSchemas;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Product-usage business numbers, read from a real PostgreSQL and published for Grafana. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ActivityMetricsTest {
    @Autowired
    private ProfileSchemas schemas;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private BusinessGaugePublisher gauges;

    @Autowired
    private MeterRegistry meters;

    private final UUID user = UUID.randomUUID();

    private void insert(UUID session, int sequence, String surface, String age) {
        jdbc.sql("""
                INSERT INTO activity.product_usage_event (user_id, event_id, schema_version, session_id, sequence,
                    event_kind, occurred_at, surface, feature, action, release_version, device_class, input_kind,
                    online, reduced_motion)
                VALUES (?, ?, 1, ?, ?, 'navigation', now() - CAST(? AS interval), ?, 'navigation', 'viewed', '0.2.203',
                        'desktop', 'pointer', true, false)""")
                .params(user, UUID.randomUUID(), session, sequence, age, surface).update();
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    @Test
    void publishesEventsBySurfaceAndSessionsForTheLastSevenDays() {
        schemas.ensureMigrated();
        var session = UUID.randomUUID();
        insert(session, 1, "calendar", "1 hour");
        insert(session, 2, null, "2 hours");
        insert(UUID.randomUUID(), 1, "calendar", "10 days");

        gauges.invalidate();
        gauges.refresh();

        var window = " FROM activity.product_usage_event WHERE occurred_at > now() - interval '7 days'";
        assertEquals(count("SELECT count(*)" + window + " AND surface = 'calendar'"),
                meters.get("yourlife.activity.events.last7days").tags("surface", "calendar").gauge().value());
        assertEquals(count("SELECT count(*)" + window + " AND surface IS NULL"),
                meters.get("yourlife.activity.events.last7days").tags("surface", "none").gauge().value());
        assertEquals(count("SELECT count(DISTINCT session_id)" + window),
                meters.get("yourlife.activity.sessions.last7days").gauge().value());
    }
}
