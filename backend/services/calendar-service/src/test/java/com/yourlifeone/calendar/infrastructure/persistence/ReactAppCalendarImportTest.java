package com.yourlifeone.calendar.infrastructure.persistence;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one-time import of the React app's calendar rows ({@code public.yourlifeone_records}), run against its
 * own schema so it cannot disturb the other tests.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ReactAppCalendarImportTest {
    private static final String SCHEMA = "calendar_import_test";

    @Autowired
    private DataSource dataSource;

    @Test
    void importsAccountsCalendarsAndEventsAndSkipsWhatCannotBeRead() {
        var jdbc = JdbcClient.create(dataSource);
        var user = UUID.randomUUID();
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.yourlifeone_records (user_id uuid, collection text, record_id text,
                    payload jsonb, deleted_at timestamptz, updated_at timestamptz DEFAULT now())""").update();
        insert(jdbc, user, "calendarAccounts", "acc-g", """
                {"id":"acc-g","name":"YourLife","email":"YourLife@Example.com","provider":"google","isPrimary":true,
                 "authStatus":"needs_reconnect","lastSyncTime":"2026-06-06T10:00:00.000Z","paletteIndex":1}""");
        insert(jdbc, user, "calendarAccounts", "acc-l", """
                {"id":"acc-l","name":"Home","email":"","provider":"local","isPrimary":false}""");
        insert(jdbc, user, "calendarAccounts", "acc-demo", """
                {"id":"acc-demo","name":"Demo","email":"demo@x","provider":"google","mocked":true}""");
        insert(jdbc, user, "calendarSources", "src-g", """
                {"id":"src-g","accountId":"acc-g","name":"YourLife","color":"#123456","visible":true,
                 "googleCalendarId":"yourlife@example.com","accessRole":"owner"}""");
        insert(jdbc, user, "calendarSources", "src-l", """
                {"id":"src-l","accountId":"acc-l","name":"Home","color":"#00ff00","visible":false}""");
        insert(jdbc, user, "calendarSources", "src-orphan", """
                {"id":"src-orphan","accountId":"acc-demo","name":"Demo","color":"#000"}""");
        insert(jdbc, user, "calendarEvents", "ev-1", """
                {"id":"ev-1","sourceId":"src-g","title":"Dentist","description":"","start":"2026-06-01T09:00:00.000Z",
                 "end":"2026-06-01T10:00:00.000Z","allDay":false,"googleEventId":"g1"}""");
        insert(jdbc, user, "calendarEvents", "ev-2", """
                {"id":"ev-2","sourceId":"src-l","title":"Trip","description":"Bags","start":"2026-07-01",
                 "end":"2026-07-03","allDay":true,"location":"Rome"}""");
        insert(jdbc, user, "calendarEvents", "ev-bad", """
                {"id":"ev-bad","sourceId":"src-l","title":"Broken","start":"soon","end":"later","allDay":false}""");

        Flyway.configure().dataSource(dataSource).schemas(SCHEMA).createSchemas(true)
                .locations("classpath:db/migration/calendar").load().migrate();

        assertEquals(2, count(jdbc, "account", user));
        assertEquals(2, count(jdbc, "source", user));
        assertEquals(2, count(jdbc, "event", user));
        assertEquals("yourlife@example.com", jdbc.sql("SELECT email FROM " + SCHEMA + ".account WHERE id = 'acc-g'")
                .query(String.class).single());
        assertEquals("needs_reconnect", jdbc.sql("SELECT auth_status FROM " + SCHEMA + ".account WHERE id = 'acc-g'")
                .query(String.class).single());
        // All-day events end at UTC midnight after their last day.
        assertEquals("2026-07-04 00:00:00", jdbc.sql("SELECT (ends_at AT TIME ZONE 'UTC')::text FROM " + SCHEMA
                + ".event WHERE id = 'ev-2'").query(String.class).single());
    }

    private static void insert(JdbcClient jdbc, UUID user, String collection, String id, String payload) {
        jdbc.sql("INSERT INTO public.yourlifeone_records (user_id, collection, record_id, payload) VALUES (?, ?, ?, ?::jsonb)")
                .params(user, collection, id, payload).update();
    }

    private static int count(JdbcClient jdbc, String table, UUID user) {
        return jdbc.sql("SELECT count(*) FROM " + SCHEMA + "." + table + " WHERE user_id = ?").param(user)
                .query(Integer.class).single();
    }
}
