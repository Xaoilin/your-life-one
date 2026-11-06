package com.yourlifeone.prayer.infrastructure.persistence;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.zaxxer.hikari.HikariDataSource;
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
 * The one-time import of the React app's reminder receipts ({@code public.yourlifeone_records}, collection
 * {@code prayerTracking}), run against its own schema so it cannot disturb the other tests, on its own
 * connections. Same configuration as {@code PrayerMetricsTest}, so both share one context.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ReactAppReminderImportTest {
    private static final UUID USER = UUID.randomUUID();
    private static final String SCHEMA = "prayer_import_test";

    @Autowired
    private DataSource dataSource;

    private DriverManagerDataSource direct() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        return new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    }

    private JdbcClient jdbc() {
        return JdbcClient.create(direct());
    }

    private void insert(JdbcClient jdbc, String recordId, String payload, boolean deleted) {
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.yourlifeone_records (user_id uuid, collection text, record_id text,
                    payload jsonb, position bigint, deleted_at timestamptz, created_at timestamptz DEFAULT now(),
                    updated_at timestamptz DEFAULT now())""").update();
        jdbc.sql("INSERT INTO public.yourlifeone_records (user_id, collection, record_id, payload, deleted_at)"
                        + " VALUES (?, 'prayerTracking', ?, ?::jsonb, " + (deleted ? "now()" : "NULL") + ")")
                .params(USER, recordId, payload).update();
    }

    /** Runs the query with {@code utc(column)} standing for the timestamp in UTC, whatever the session zone. */
    private static String one(JdbcClient jdbc, String sql) {
        var utc = sql.replaceAll("utc\\((\\w+)\\)", "to_char($1 AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI:SS')");
        return jdbc.sql(utc).param(USER).query(String.class).single();
    }

    @Test
    void importsEveryReminderReceiptWithItsSnooze() {
        var jdbc = jdbc();
        insert(jdbc, "meta", "{\"schemaVersion\":3}", false);
        insert(jdbc, "reminder:2026-09-25::Fajr::2026-09-25T05:51:00.000Z", """
                {"date":"2026-09-25","prayerName":"Fajr","deadlineAt":"2026-09-25T05:51:00.000Z",
                 "notificationKey":"prayer:2026-09-25:Fajr:1790315460000","notifiedAt":"2026-09-25T05:36:02.000Z"}""", false);
        // The same prayer again with an earlier deadline (a timetable change): the latest deadline is kept.
        insert(jdbc, "reminder:2026-09-25::Fajr::2026-09-25T05:50:00.000Z", """
                {"date":"2026-09-25","prayerName":"Fajr","deadlineAt":"2026-09-25T05:50:00.000Z",
                 "notificationKey":"prayer:2026-09-25:Fajr:1790315400000"}""", false);
        insert(jdbc, "reminder:2026-09-25::Asr::2026-09-25T18:12:00.000Z", """
                {"date":"2026-09-25","prayerName":"Asr","deadlineAt":"2026-09-25T18:12:00.000Z",
                 "notificationKey":"prayer:2026-09-25:Asr:1790359920000","notifiedAt":"2026-09-25T17:57:00.000Z",
                 "snoozedUntil":"2026-09-25T18:02:00.000Z"}""", false);
        insert(jdbc, "reminder:2026-09-24::Isha::2026-09-24T23:52:00.000Z", """
                {"date":"2026-09-24","prayerName":"Isha","deadlineAt":"2026-09-24T23:52:00.000Z",
                 "notificationKey":"x"}""", true);
        insert(jdbc, "bounded:bounded:v1:prayer-opportunity:2026-09-25:Dhuhr:1790337120000", """
                {"notificationKey":"bounded:v1:prayer-opportunity:2026-09-25:Dhuhr:1790337120000","date":"2026-09-25",
                 "kind":"prayer-opportunity","attemptedAt":"2026-09-25T11:52:05.000Z","snoozeCount":0}""", false);
        insert(jdbc, "bounded:bounded:v1:momentum:2026-09-25:learn:Maghrib:1790359920000", """
                {"notificationKey":"bounded:v1:momentum:2026-09-25:learn:Maghrib:1790359920000","date":"2026-09-25",
                 "kind":"momentum","attemptedAt":"2026-09-25T18:12:01.000Z","notifiedAt":"2026-09-25T18:12:02.000Z",
                 "snoozedUntil":"2026-09-25T18:17:02.000Z","snoozeCount":1}""", false);
        insert(jdbc, "bounded:bounded:v1:momentum:2026-09-25:move:Maghrib:1790359920000", """
                {"notificationKey":"bounded:v1:momentum:2026-09-25:move:Maghrib:1790359920000","date":"2026-09-25",
                 "kind":"momentum","attemptedAt":"2026-09-25T18:12:01.000Z","snoozeCount":0}""", false);

        Flyway.configure().dataSource(direct())
                .schemas(SCHEMA).createSchemas(true).locations("classpath:db/migration/prayer").load().migrate();

        assertEquals("2", one(jdbc, "SELECT count(*)::text FROM " + SCHEMA + ".reminder WHERE user_id = ?"));
        assertEquals("2026-09-25 05:51:00|2026-09-25 05:36:02|0", one(jdbc, "SELECT utc(deadline_at) || '|'"
                + " || utc(sent_at) || '|' || snooze_count FROM " + SCHEMA + ".reminder"
                + " WHERE user_id = ? AND prayer_name = 'Fajr'"));
        assertEquals("2026-09-25 18:02:00|1|2026-09-25 18:02:00", one(jdbc, "SELECT utc(snoozed_until) || '|'"
                + " || snooze_count || '|' || utc(resent_at) FROM " + SCHEMA + ".reminder"
                + " WHERE user_id = ? AND prayer_name = 'Asr'"));

        assertEquals("3", one(jdbc, "SELECT count(*)::text FROM " + SCHEMA + ".notice WHERE user_id = ?"));
        assertEquals("prayer-opportunity|Dhuhr||2026-09-25 11:52:00|2026-09-25 12:22:00|2026-09-25 11:52:05",
                one(jdbc, "SELECT kind || '|' || prayer_name || '|' || coalesce(pillar, '') || '|' || utc(fires_at)"
                        + " || '|' || utc(expires_at) || '|' || utc(sent_at) FROM " + SCHEMA + ".notice"
                        + " WHERE user_id = ? AND key = 'bounded:v1:prayer-opportunity:2026-09-25:Dhuhr:1790337120000'"));
        assertEquals("learn|Maghrib|2026-09-25 18:12:02|1", one(jdbc, "SELECT pillar || '|' || prayer_name || '|'"
                + " || utc(sent_at) || '|' || snooze_count FROM " + SCHEMA + ".notice"
                + " WHERE user_id = ? AND key LIKE 'bounded:v1:momentum:%:learn:%'"));
    }
}
