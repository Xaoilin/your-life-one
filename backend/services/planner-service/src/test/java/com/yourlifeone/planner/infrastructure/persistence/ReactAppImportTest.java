package com.yourlifeone.planner.infrastructure.persistence;

import com.yourlifeone.planner.testing.PlannerTest;
import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one-time imports of the React app's tasks, progress and clock ({@code public.yourlifeone_records}), each run
 * against its own schema so they cannot disturb the other tests. They use their own connections: the
 * service's two-connection pool may be busy migrating.
 */
@PlannerTest
class ReactAppImportTest {
    private static final UUID USER = UUID.randomUUID();

    @Autowired
    private DataSource dataSource;

    private DriverManagerDataSource direct() {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));
        return new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    }

    private JdbcClient jdbc() {
        return JdbcClient.create(direct());
    }

    private void migrate(String domain, String schema) {
        Flyway.configure().dataSource(direct()).schemas(schema).createSchemas(true)
                .locations("classpath:db/migration/" + domain).load().migrate();
    }

    private void insert(JdbcClient jdbc, String collection, String id, String payload, boolean deleted) {
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS public.yourlifeone_records (user_id uuid, collection text, record_id text,
                    payload jsonb, position bigint, deleted_at timestamptz, created_at timestamptz DEFAULT now(),
                    updated_at timestamptz DEFAULT now())""").update();
        jdbc.sql("INSERT INTO public.yourlifeone_records (user_id, collection, record_id, payload, deleted_at)"
                        + " VALUES (?, ?, ?, ?::jsonb, " + (deleted ? "now()" : "NULL") + ")")
                .params(USER, collection, id, payload).update();
    }

    private static String one(JdbcClient jdbc, String sql) {
        return jdbc.sql(sql).param(USER).query(String.class).single();
    }

    @Test
    void importsTasksTheWayTheAppShowedThem() {
        var jdbc = jdbc();
        insert(jdbc, "tasks", "fajr", """
                {"id":"fajr","title":"Fajr Prayer","category":"daily","priority":"high","completed":false,
                 "completedAt":"2026-09-26T05:20:34.637Z","recurring":{"frequency":"daily","lastReset":"2026-09-27"},
                 "createdAt":"2026-03-29T22:31:07.725Z","updatedAt":"2026-09-26T05:20:34.637Z","description":""}""", false);
        insert(jdbc, "tasks", "done", """
                {"id":"done","title":"Fix the door","category":"task","priority":"low","completed":true,
                 "completedAt":"2026-07-05T11:59:41.710Z","dueDate":"2026-07-06","description":"lock"}""", false);
        insert(jdbc, "tasks", "goal", """
                {"id":"goal","title":"Finish the Quran","goalTag":"Islam","category":"goal","priority":"medium",
                 "completed":false,"description":""}""", false);
        insert(jdbc, "tasks", "gone", "{\"id\":\"gone\",\"title\":\"Deleted\",\"category\":\"task\"}", true);

        migrate("tasks", "tasks_import_test");

        assertEquals("3", one(jdbc, "SELECT count(*)::text FROM tasks_import_test.task WHERE user_id = ?"));
        assertEquals("prayer|Fajr|false|2026-09-27|", one(jdbc, "SELECT category || '|' || prayer_name || '|' || completed"
                + " || '|' || last_reset || '|' || coalesce(completed_at::text, '')"
                + " FROM tasks_import_test.task WHERE user_id = ? AND id = 'fajr'"));
        assertEquals("true|2026-07-05|UTC|2026-07-06", one(jdbc, "SELECT completed || '|' || completed_local_date"
                + " || '|' || completion_time_zone || '|' || due_date FROM tasks_import_test.task WHERE user_id = ? AND id = 'done'"));
    }

    @Test
    void importsProgressWithEveryPrayerReceiptAndDailyMomentum() {
        var jdbc = jdbc();
        insert(jdbc, "gamification", "profile", """
                {"level":21,"totalXp":12021,"currentStreak":18,"longestStreak":18,"lastCompletionDate":"2026-09-27",
                 "totalTasksCompleted":378,"badges":["first-blood","xp-50","first-blood"],
                 "dailyMomentumLearn":{"schemaVersion":2,"templates":[],"logs":{},"reminderPreferences":{}},
                 "lifeHeroAdventure":{"round":1}}""", false);
        insert(jdbc, "gamification", "habit:water", "{\"count\":4}", false);
        insert(jdbc, "gamification", "day:2026-09-26", "{\"taskIds\":[\"water\",\"fajr\"]}", false);
        insert(jdbc, "gamification", "prayer:2026-09-26::Fajr", """
                {"date":"2026-09-26","prayerName":"Fajr","status":"on_time","rewarded":true,"taskId":"fajr",
                 "recordedAt":"2026-09-26T05:20:34.637Z","xpEarned":50}""", false);
        insert(jdbc, "gamification", "prayer:2026-09-25::Isha", """
                {"date":"2026-09-25","prayerName":"Isha","status":"missed","rewarded":false,"taskId":"isha",
                 "recordedAt":"2026-09-26T00:20:00Z"}""", false);

        migrate("gamification", "gamification_import_test");

        assertEquals("12021|21|18|378|2", one(jdbc, "SELECT total_xp || '|' || level || '|' || current_streak || '|'"
                + " || total_tasks_completed || '|' || cardinality(badges) FROM gamification_import_test.profile WHERE user_id = ?"));
        assertEquals("4", one(jdbc, "SELECT count::text FROM gamification_import_test.habit_tally WHERE user_id = ?"));
        assertEquals("2", one(jdbc, "SELECT count(*)::text FROM gamification_import_test.daily_log WHERE user_id = ?"));
        assertEquals("2|50", one(jdbc, "SELECT count(*) || '|' || sum(xp_earned)"
                + " FROM gamification_import_test.prayer_reward WHERE user_id = ? AND source = 'import'"));
        assertEquals("learn", one(jdbc, "SELECT string_agg(pillar, ',') FROM gamification_import_test.momentum WHERE user_id = ?"));
    }

    @Test
    void importsTheClockWorkspace() {
        var jdbc = jdbc();
        insert(jdbc, "clock", "meta", "{\"nextTimerNumber\":12,\"nextStopwatchNumber\":8}", false);
        insert(jdbc, "clock", "stopwatch:s1", """
                {"id":"s1","laps":[1200,2400],"label":"Bunshin","startedAt":null,"accumulatedMs":90590}""", false);
        insert(jdbc, "clock", "timer:t1", """
                {"id":"t1","label":"World Tournament","sound":"chime","endsAt":null,"status":"completed","alerting":true,
                 "durationMs":420000,"completedAt":"2026-08-24T03:17:06.975Z","remainingMs":0}""", false);
        insert(jdbc, "clock", "timer:t2", """
                {"id":"t2","label":"Stuck","sound":"gong","endsAt":null,"status":"running","durationMs":300000,
                 "remainingMs":300000}""", false);

        migrate("clock", "clock_import_test");

        assertEquals("8|12", one(jdbc, "SELECT next_stopwatch_number || '|' || next_timer_number"
                + " FROM clock_import_test.counter WHERE user_id = ?"));
        assertEquals("Bunshin|90590|{1200,2400}", one(jdbc, "SELECT label || '|' || accumulated_ms || '|' || laps::text"
                + " FROM clock_import_test.stopwatch WHERE user_id = ?"));
        assertEquals("completed,idle", one(jdbc, "SELECT string_agg(status, ',' ORDER BY id)"
                + " FROM clock_import_test.timer WHERE user_id = ?"));
        assertEquals("chime", one(jdbc, "SELECT sound FROM clock_import_test.timer WHERE user_id = ? AND id = 't2'"));
    }
}
