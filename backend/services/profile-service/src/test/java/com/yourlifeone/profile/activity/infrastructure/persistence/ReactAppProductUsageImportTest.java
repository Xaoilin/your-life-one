package com.yourlifeone.profile.activity.infrastructure.persistence;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one-time import of the React app's product-usage events ({@code public.product_usage_events}),
 * run against its own schema so it cannot disturb the other tests. It uses its own connections: the
 * service's two-connection pool may be busy migrating the service schemas in the background.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ReactAppProductUsageImportTest {
    private static final String SCHEMA = "activity_import_test";

    @Autowired
    private DataSource dataSource;

    @Test
    void copiesEveryEventOnceKeepingItsValuesAndIsANoOpWhenRunAgain() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        var direct = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
        var jdbc = JdbcClient.create(direct);
        var user = UUID.randomUUID();
        var session = UUID.randomUUID();
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.product_usage_events (
                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL, event_id uuid NOT NULL,
                    schema_version smallint NOT NULL, session_id uuid NOT NULL, sequence integer NOT NULL,
                    event_kind text NOT NULL, occurred_at timestamptz NOT NULL, surface text, feature text NOT NULL,
                    action text NOT NULL, outcome text, duration_ms integer, error_code text, target text,
                    release_version text NOT NULL, device_class text NOT NULL, input_kind text NOT NULL,
                    online boolean NOT NULL, reduced_motion boolean NOT NULL, metadata jsonb NOT NULL DEFAULT '{}',
                    received_at timestamptz NOT NULL DEFAULT now())""").update();
        insert(jdbc, user, session, 1, "session", null, null, null, "{}");
        insert(jdbc, user, session, 2, "error", "calendar", "failure", "calendar_render",
                "{\"previousSurface\":\"dashboard\",\"retryCount\":2}");
        var flyway = Flyway.configure().dataSource(direct).schemas(SCHEMA).createSchemas(true)
                .locations("classpath:db/migration/activity").load();

        flyway.migrate();

        assertEquals(List.of(
                        "1|session|none|none|none|{}",
                        "2|error|calendar|failure|calendar_render|{\"retryCount\": 2, \"previousSurface\": \"dashboard\"}"),
                jdbc.sql("SELECT sequence || '|' || event_kind || '|' || coalesce(surface, 'none') || '|'"
                                + " || coalesce(outcome, 'none') || '|' || coalesce(error_code, 'none') || '|' || metadata"
                                + " FROM " + SCHEMA + ".product_usage_event WHERE user_id = ? ORDER BY sequence")
                        .param(user).query(String.class).list());

        // A second run (for example after a failed deploy) never copies an event twice.
        jdbc.sql("DELETE FROM " + SCHEMA + ".flyway_schema_history WHERE version = '2'").update();
        flyway.migrate();
        assertEquals(2, jdbc.sql("SELECT count(*) FROM " + SCHEMA + ".product_usage_event WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }

    private static void insert(JdbcClient jdbc, UUID user, UUID session, int sequence, String kind, String surface,
            String outcome, String errorCode, String metadata) {
        jdbc.sql("""
                INSERT INTO public.product_usage_events (user_id, event_id, schema_version, session_id, sequence,
                    event_kind, occurred_at, surface, feature, action, outcome, error_code, release_version,
                    device_class, input_kind, online, reduced_motion, metadata)
                VALUES (?, ?, 1, ?, ?, ?, now(), ?, 'navigation', 'viewed', ?, ?, '0.2.200', 'desktop', 'pointer',
                        true, false, ?::jsonb)""")
                .params(user, UUID.randomUUID(), session, sequence, kind, surface, outcome, errorCode, metadata)
                .update();
    }
}
