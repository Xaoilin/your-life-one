package com.yourlifeone.prayer.infrastructure.persistence;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exercises the real V5-to-V6 upgrade and the exact controller rollback script on invented identities. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class OrphanContainmentIntegrationTest {
    private static final String SCHEMA = "prayer_containment_test";

    @Autowired
    private DataSource dataSource;

    @Test
    void filteredIdentityVisibilityAbortsContainment() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                statement.execute("CREATE ROLE containment_blind");
                statement.execute("GRANT USAGE ON SCHEMA auth TO containment_blind");
                statement.execute("GRANT SELECT ON auth.users TO containment_blind");
                statement.execute("ALTER TABLE auth.users ENABLE ROW LEVEL SECURITY");
                statement.execute("SET LOCAL ROLE containment_blind");
                var resource = new ClassPathResource("db/migration/prayer/V6__contain_orphan_profiles.sql");
                try (var input = resource.getInputStream()) {
                    var sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    var failure = assertThrows(SQLException.class, () -> statement.execute(sql));
                    assertEquals("P0001", failure.getSQLState());
                }
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void containsOnlyVerifiedOrphansAndRestoresOnlyUnchangedContainedPreferences() throws Exception {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        var direct = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
        var jdbc = JdbcClient.create(direct);
        migrate(direct, "5");
        // V5 can import reminder fixtures created by earlier tests in the shared PostgreSQL container.
        var importedReminders = count(jdbc, "reminder");
        var valid = UUID.randomUUID();
        var orphan = UUID.randomUUID();
        var edited = UUID.randomUUID();
        var disabled = UUID.randomUUID();
        jdbc.sql("INSERT INTO auth.users (id) VALUES (?)").param(valid).update();
        for (var user : new UUID[]{valid, orphan, edited, disabled}) {
            jdbc.sql("INSERT INTO " + SCHEMA + ".profile"
                    + " (user_id, enabled, reminder_enabled, reminder_minutes, tracking_started_at, updated_at)"
                    + " VALUES (?, ?, true, 15, '2026-09-25T04:00:00Z', '2026-09-25T04:00:00Z')")
                    .params(user, !user.equals(disabled)).update();
            jdbc.sql("INSERT INTO " + SCHEMA + ".reminder"
                    + " (user_id, prayer_date, prayer_name, deadline_at, sent_at)"
                    + " VALUES (?, '2026-09-25', 'Fajr', '2026-09-25T05:51:00Z', '2026-09-25T05:40:00Z')")
                    .param(user).update();
        }

        migrate(direct, "6");
        assertEquals(true, enabled(jdbc, valid));
        assertEquals(false, enabled(jdbc, orphan));
        assertEquals(false, enabled(jdbc, edited));
        assertEquals(false, enabled(jdbc, disabled));
        assertEquals(2, count(jdbc, "orphan_profile_containment"));
        assertEquals(importedReminders + 4, count(jdbc, "reminder"));
        // A later preference write must survive rollback.
        jdbc.sql("UPDATE " + SCHEMA + ".profile SET updated_at = updated_at + interval '1 second' WHERE user_id = ?")
                .param(edited).update();

        try (var connection = direct.getConnection()) {
            connection.setAutoCommit(false);
            rollback(connection);
            rollback(connection); // Idempotent: the before-image and restoration receipt are retained.
            connection.commit();
        }
        assertEquals(true, enabled(jdbc, valid));
        assertEquals(true, enabled(jdbc, orphan));
        assertEquals(false, enabled(jdbc, edited));
        assertEquals(false, enabled(jdbc, disabled));
        assertEquals(importedReminders + 4, count(jdbc, "reminder"));
        assertEquals(1, jdbc.sql("SELECT count(*) FROM " + SCHEMA
                + ".orphan_profile_containment WHERE restored_at IS NOT NULL").query(Integer.class).single());
        assertEquals("2026-09-25 04:00:00", jdbc.sql("SELECT to_char(updated_at AT TIME ZONE 'UTC',"
                + " 'YYYY-MM-DD HH24:MI:SS') FROM " + SCHEMA + ".profile WHERE user_id = ?")
                .param(orphan).query(String.class).single());
    }

    private static void migrate(DataSource source, String target) {
        Flyway.configure().dataSource(source).schemas(SCHEMA).createSchemas(true).target(target)
                .locations("classpath:db/migration/prayer").load().migrate();
    }

    private static boolean enabled(JdbcClient jdbc, UUID user) {
        return jdbc.sql("SELECT enabled FROM " + SCHEMA + ".profile WHERE user_id = ?")
                .param(user).query(Boolean.class).single();
    }

    private static int count(JdbcClient jdbc, String table) {
        return jdbc.sql("SELECT count(*) FROM " + SCHEMA + "." + table).query(Integer.class).single();
    }

    private static void rollback(Connection connection) throws Exception {
        var root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("scripts/rollback-prayer-orphan-containment.sql"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("Cannot find the controller rollback script");
        var path = root.resolve("scripts/rollback-prayer-orphan-containment.sql");
        try (var statement = connection.createStatement()) {
            statement.execute("SET LOCAL search_path = " + SCHEMA);
            statement.execute(Files.readString(path));
        }
    }
}
