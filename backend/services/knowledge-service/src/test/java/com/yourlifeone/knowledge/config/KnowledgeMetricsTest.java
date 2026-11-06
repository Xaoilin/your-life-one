package com.yourlifeone.knowledge.config;

import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.knowledge.infrastructure.persistence.KnowledgeSchemas;
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
class KnowledgeMetricsTest {
    @Autowired
    private KnowledgeSchemas schemas;

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
    void publishesKnowledgeLifestyleAndProjectNumbers() {
        schemas.ensureMigrated();
        jdbc.sql("""
                INSERT INTO knowledge.topic (user_id, id, name, sort_order, created_at, updated_at)
                VALUES (?, 't1', 'Prayer', 0, now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO knowledge.entry (user_id, id, topic_id, title, sources, created_at, updated_at)
                VALUES (?, 'e1', 't1', 'On time', '[{"type":"quran","surah":4}]', now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO lifestyle.item (user_id, id, type, title, status, sort_order, created_at, updated_at)
                VALUES (?, 'l1', 'haram', 'Backbiting', 'mastered', 0, now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO projects.project (user_id, id, catalog_key, name, kind, status, sort_order, preview,
                                              created_at, updated_at)
                VALUES (?, 'p1', 'custom:p1', 'Board', 'web_app', 'active', 0, '{}', now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO projects.page (user_id, id, project_id, title, is_overview, created_at, updated_at)
                VALUES (?, 'o1', 'p1', 'Overview', true, now(), now())""").param(user).update();

        gauges.invalidate();
        gauges.refresh();

        assertEquals(count("SELECT count(*) FROM knowledge.entry"), gauge("yourlife.knowledge.records", "kind", "entry"));
        assertEquals(count("SELECT count(*) FROM knowledge.entry, jsonb_array_elements(sources) s"
                + " WHERE s ->> 'type' = 'quran'"), gauge("yourlife.knowledge.sources", "type", "quran"));
        assertEquals(count("SELECT count(*) FROM lifestyle.item WHERE type = 'haram' AND status = 'mastered'"),
                gauge("yourlife.lifestyle.items", "type", "haram", "status", "mastered"));
        assertEquals(count("SELECT count(*) FROM projects.project WHERE status = 'active' AND kind = 'web_app'"),
                gauge("yourlife.projects.projects", "status", "active", "kind", "web_app"));
        assertEquals(count("SELECT count(*) FROM projects.page WHERE updated_at > now() - interval '30 days'"),
                gauge("yourlife.projects.pages.updated.last30days"));
    }
}
