package com.yourlifeone.profile.infrastructure.persistence;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/** Bounded synthetic query/index evidence; temporary rows are rolled back and never represent an account. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ViewPreferencesQueryPlanTest {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private ProfileSchemas schemas;

    @Test
    void existingCompositePrimaryKeyCoversReadsAndUpsertsWithoutAnotherIndex() throws Exception {
        schemas.ensureMigrated();
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("SET LOCAL statement_timeout = '5s'");
                statement.execute("CREATE TEMP TABLE view_preferences_plan (LIKE profile.view_preferences INCLUDING ALL) ON COMMIT DROP");
                statement.execute("""
                        INSERT INTO view_preferences_plan (user_id, preference_key, preference_value, updated_at)
                        SELECT md5(owner_number::text)::uuid, 'choice.' || preference_number, 'day', CURRENT_TIMESTAMP
                        FROM generate_series(1, 200) owner_number CROSS JOIN generate_series(1, 32) preference_number
                        """);
                statement.execute("ANALYZE view_preferences_plan");
                var read = plan(connection, """
                        SELECT user_id, preference_key, preference_value, updated_at
                        FROM view_preferences_plan WHERE user_id = md5('50')::uuid ORDER BY preference_key
                        """);
                var write = plan(connection, """
                        INSERT INTO view_preferences_plan (user_id, preference_key, preference_value, updated_at)
                        VALUES (md5('50')::uuid, 'choice.1', 'week', CURRENT_TIMESTAMP)
                        ON CONFLICT (user_id, preference_key)
                        DO UPDATE SET preference_value = EXCLUDED.preference_value, updated_at = EXCLUDED.updated_at
                        """);
                assertThat(read).contains("view_preferences_plan_pkey");
                assertThat(write).contains("view_preferences_plan_pkey");
                System.out.println("VIEW_PREFERENCES_PLAN: 6400 synthetic rows; read\n" + read + "\nupsert\n" + write);
            } finally {
                connection.rollback();
            }
        }
    }

    private static String plan(Connection connection, String query) throws SQLException {
        var result = new StringBuilder();
        try (var statement = connection.createStatement();
                var rows = statement.executeQuery("EXPLAIN (ANALYZE, BUFFERS) " + query)) {
            while (rows.next()) result.append(rows.getString(1)).append('\n');
        }
        return result.toString();
    }
}
