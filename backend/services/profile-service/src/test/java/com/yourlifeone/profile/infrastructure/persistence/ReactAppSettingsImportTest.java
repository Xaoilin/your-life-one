package com.yourlifeone.profile.infrastructure.persistence;

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
 * The one-time import of the React app's settings and integrations ({@code public.yourlifeone_records}), run
 * against its own schema so it cannot disturb the other tests. It uses its own connections: the
 * service's two-connection pool may be busy migrating the service schema in the background.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ReactAppSettingsImportTest {
    private static final String SCHEMA = "profile_import_test";

    @Autowired
    private DataSource dataSource;

    @Test
    void importsPreferencesGoalTagsAndGoogleIntegrationAndSkipsWhatCannotBeRead() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        var direct = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
        var jdbc = JdbcClient.create(direct);
        var user = UUID.randomUUID();
        var messy = UUID.randomUUID();
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.yourlifeone_records (user_id uuid, collection text, record_id text,
                    payload jsonb, deleted_at timestamptz, updated_at timestamptz DEFAULT now())""").update();
        insert(jdbc, user, "settings", "singleton", """
                {"theme":"light","dataRetentionDays":30,"telemetry":true,"defaultCalendarTab":"agenda",
                 "goalTags":[" Health ","Work","Health",""],"lifeHeroEnabled":false,"assistantProvider":"hosted"}""");
        insert(jdbc, messy, "settings", "singleton", """
                {"theme":"neon","dataRetentionDays":"lots","telemetry":"yes","defaultCalendarTab":"year",
                 "goalTags":"Health"}""");
        insert(jdbc, user, "integrations", "int-google", """
                {"id":"int-google","provider":"google","status":"connected","configuredAt":"2026-09-01T10:00:00.000Z"}""");
        insert(jdbc, user, "integrations", "int-github", """
                {"id":"int-github","provider":"github","status":"connected"}""");
        insert(jdbc, messy, "integrations", "int-google", """
                {"id":"int-google","provider":"google","status":"mocked","configuredAt":"not a date"}""");

        Flyway.configure().dataSource(direct).schemas(SCHEMA).createSchemas(true)
                .locations("classpath:db/migration/profile").load().migrate();

        assertEquals("light|30|true|agenda", preferences(jdbc, user));
        assertEquals("dark|90|false|null", preferences(jdbc, messy));
        assertEquals(List.of("Health", "Work"), jdbc.sql("SELECT name FROM " + SCHEMA
                + ".goal_tag WHERE user_id = ? ORDER BY position").param(user).query(String.class).list());
        assertEquals(0, count(jdbc, "goal_tag", messy));
        assertEquals(List.of("google:connected"), integrations(jdbc, user));
        assertEquals(List.of("google:disconnected"), integrations(jdbc, messy));
    }

    private static void insert(JdbcClient jdbc, UUID user, String collection, String id, String payload) {
        jdbc.sql("INSERT INTO public.yourlifeone_records (user_id, collection, record_id, payload) VALUES (?, ?, ?, ?::jsonb)")
                .params(user, collection, id, payload).update();
    }

    private static String preferences(JdbcClient jdbc, UUID user) {
        return jdbc.sql("SELECT theme || '|' || data_retention_days || '|' || telemetry || '|'"
                        + " || coalesce(default_calendar_tab, 'null') FROM " + SCHEMA + ".app_preferences WHERE user_id = ?")
                .param(user).query(String.class).single();
    }

    private static List<String> integrations(JdbcClient jdbc, UUID user) {
        return jdbc.sql("SELECT provider || ':' || status FROM " + SCHEMA + ".integration WHERE user_id = ?")
                .param(user).query(String.class).list();
    }

    private static int count(JdbcClient jdbc, String table, UUID user) {
        return jdbc.sql("SELECT count(*) FROM " + SCHEMA + "." + table + " WHERE user_id = ?").param(user)
                .query(Integer.class).single();
    }
}
