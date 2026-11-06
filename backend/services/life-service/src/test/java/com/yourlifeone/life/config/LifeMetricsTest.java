package com.yourlifeone.life.config;

import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.life.infrastructure.persistence.LifeSchemas;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Each domain's business numbers, read from a real PostgreSQL and published for Grafana. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class LifeMetricsTest {
    @Autowired
    private LifeSchemas schemas;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private BusinessGaugePublisher gauges;

    @Autowired
    private MeterRegistry meters;

    private final UUID user = UUID.randomUUID();

    private double gauge(String name, String... tags) {
        return meters.get(name).tags(tags).gauge().value();
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    @Test
    void publishesInventoryTripsHealthAndJobsNumbers() {
        schemas.ensureMigrated();
        jdbc.sql("""
                INSERT INTO inventory.need (user_id, id, name, category, required_quantity, unit, priority, status,
                                            created_at, updated_at)
                VALUES (?, 'n1', 'Screws', 'fastener', 1, 'pcs', 'normal', 'needed', now(), now())""")
                .param(user).update();
        jdbc.sql("""
                INSERT INTO trips.trip (user_id, id, name, status, start_date, end_date, budget_currency, budget_total,
                                        created_at, updated_at)
                VALUES (?, 't1', 'Next trip', 'booked', current_date + 10, current_date + 20, 'GBP', 0, now(), now())""")
                .param(user).update();
        jdbc.sql("""
                INSERT INTO trips.budget_entry (user_id, id, trip_id, title, category, amount, status, created_at,
                                                updated_at)
                VALUES (?, 'b1', 't1', 'Food', 'food', 1250, 'planned', now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO health.fast_food_entry (user_id, id, venue, entry_date, rating, created_at, updated_at)
                VALUES (?, 'f1', 'Burger place', current_date, 'awful', now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO jobs.application (user_id, id, position, company, role, work_type, remote_region,
                    remote_status, remote_evidence, status, next_action, created_at, updated_at)
                VALUES (?, 'j1', 0, 'Acme', 'Engineer', 'unknown', 'unknown', 'needs_verification', 'Unknown',
                        'interview', 'Prepare', now(), now())""").param(user).update();

        gauges.invalidate();
        gauges.refresh();

        assertEquals(count("SELECT count(*) FROM inventory.need WHERE status = 'needed' AND category = 'fastener'"),
                gauge("yourlife.inventory.needs.open", "category", "fastener", "status", "needed"));
        assertEquals(count("SELECT count(*) FROM trips.trip WHERE status = 'booked' AND end_date >= current_date"),
                gauge("yourlife.trips.upcoming", "status", "booked"));
        assertEquals(count("SELECT coalesce(sum(b.amount), 0) FROM trips.budget_entry b JOIN trips.trip t"
                        + " ON t.user_id = b.user_id AND t.id = b.trip_id WHERE b.category = 'food'"
                        + " AND t.end_date >= current_date") / 100.0,
                gauge("yourlife.trips.budget.forecast", "category", "food", "currency", "GBP"));
        assertEquals(count("SELECT count(*) FROM health.fast_food_entry WHERE rating = 'awful'"
                        + " AND entry_date > current_date - 7"),
                gauge("yourlife.health.fastfood.entries.last7days", "rating", "awful"));
        assertEquals(count("SELECT count(*) FROM jobs.application WHERE status = 'interview'"),
                gauge("yourlife.jobs.applications", "status", "interview"));
    }
}
