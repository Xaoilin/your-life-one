package com.yourlifeone.planner.tasks.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Task settings and their numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TasksProperties.class)
public class TasksConfiguration {
    /** What the user keeps: tasks by category (a small configured set) and whether they are done. */
    @Bean
    BusinessGauge tasksGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.tasks.tasks", "Tasks, habits and goals by category and state",
                () -> jdbc.sql("""
                        SELECT category, CASE WHEN completed THEN 'done' ELSE 'open' END AS state, count(*) AS total
                        FROM tasks.task
                        GROUP BY 1, 2""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"),
                                "category", rs.getString("category"), "state", rs.getString("state")))
                        .list());
    }

    /** How much gets done: rewarded habit completions over the last week, from the daily log. */
    @Bean
    BusinessGauge habitCompletionsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.tasks.habit.completions.last7days", "Habit completions in the last 7 days",
                () -> jdbc.sql("SELECT count(*) FROM gamification.daily_log WHERE day > current_date - 7")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong(1)))
                        .list());
    }

    /** One-off tasks and goals finished over the last week, by category. */
    @Bean
    BusinessGauge tasksCompletedGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.tasks.completed.last7days", "Tasks and goals completed in the last 7 days",
                () -> jdbc.sql("""
                        SELECT category, count(*) AS total
                        FROM tasks.task
                        WHERE completed AND completed_at > now() - interval '7 days' AND category IN ('task', 'goal')
                        GROUP BY category""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "category", rs.getString("category")))
                        .list());
    }
}
