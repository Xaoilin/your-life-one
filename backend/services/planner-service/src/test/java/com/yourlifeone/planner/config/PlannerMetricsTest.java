package com.yourlifeone.planner.config;

import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.planner.infrastructure.persistence.PlannerSchemas;
import com.yourlifeone.planner.testing.PlannerTest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Each domain's business numbers, read from a real PostgreSQL and published for Grafana. */
@PlannerTest
class PlannerMetricsTest {
    @Autowired
    private PlannerSchemas schemas;

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
    void publishesTaskProgressAndClockNumbers() {
        schemas.ensureMigrated();
        jdbc.sql("""
                INSERT INTO tasks.task (user_id, id, title, category, priority, completed, completed_at, completed_local_date,
                                        completion_time_zone, recurrence, created_at, updated_at)
                VALUES (?, 'h1', 'Water', 'daily', 'high', true, now(), current_date, 'UTC', 'daily', now(), now())""")
                .param(user).update();
        jdbc.sql("INSERT INTO gamification.daily_log (user_id, day, task_id) VALUES (?, current_date, 'h1')").param(user).update();
        jdbc.sql("""
                INSERT INTO gamification.profile (user_id, total_xp, level, current_streak, longest_streak, updated_at)
                VALUES (?, 900000, 189, 400, 400, now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO gamification.prayer_reward (user_id, prayer_date, prayer_name, source, log_id, level,
                    current_streak, recorded_at, granted_at)
                VALUES (?, current_date, 'Isha', 'planner', 'prayer:isha', 1, 1, now(), now())""").param(user).update();
        jdbc.sql("""
                INSERT INTO clock.timer (user_id, id, label, duration_ms, remaining_ms, ends_at, status, sound, position,
                                         created_at, updated_at)
                VALUES (?, 't1', 'Tea', 60000, 60000, 1790000000000, 'running', 'chime', 0, now(), now())""")
                .param(user).update();

        gauges.invalidate();
        gauges.refresh();

        assertEquals(count("SELECT count(*) FROM tasks.task WHERE category = 'daily' AND completed"),
                gauge("yourlife.tasks.tasks", "category", "daily", "state", "done"));
        assertEquals(count("SELECT count(*) FROM gamification.daily_log WHERE day > current_date - 7"),
                gauge("yourlife.tasks.habit.completions.last7days"));
        assertEquals(900000, gauge("yourlife.gamification.progress", "measure", "xp"));
        assertEquals(400, gauge("yourlife.gamification.progress", "measure", "current_streak"));
        assertEquals(count("SELECT count(*) FROM gamification.prayer_reward WHERE prayer_name = 'Isha'"
                + " AND prayer_date > current_date - 30"), gauge("yourlife.gamification.prayer.rewards.last30days", "prayer", "Isha"));
        assertEquals(count("SELECT count(*) FROM clock.timer WHERE status = 'running'"),
                gauge("yourlife.clock.items", "kind", "timer", "running", "true"));
    }
}
