package com.yourlifeone.planner.tasks.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Task categories, priorities and board states ({@code app.planner.tasks.*}).
 *
 * @param categories every category; {@code daily} and {@code prayer} are habits
 * @param frequencies how a daily habit may repeat
 * @param workflowStates project-board columns; the first is where a task joins a board
 * @param prayerNames the prayers a prayer habit may track
 * @param rolloverInterval how often every user's habits are checked for a new day
 */
@ConfigurationProperties("app.planner.tasks")
public record TasksProperties(
        List<String> categories,
        List<String> priorities,
        List<String> frequencies,
        List<String> workflowStates,
        List<String> prayerNames,
        Duration rolloverInterval) {
    public TasksProperties {
        categories = copy(categories);
        priorities = copy(priorities);
        frequencies = copy(frequencies);
        workflowStates = copy(workflowStates);
        prayerNames = copy(prayerNames);
        rolloverInterval = rolloverInterval == null ? Duration.ofMinutes(5) : rolloverInterval;
        if (frequencies.isEmpty() || workflowStates.isEmpty()) {
            throw new IllegalArgumentException("app.planner.tasks needs frequencies and workflow states");
        }
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
