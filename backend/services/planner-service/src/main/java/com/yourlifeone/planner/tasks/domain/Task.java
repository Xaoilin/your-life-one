package com.yourlifeone.planner.tasks.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/**
 * A task, daily habit, prayer habit or goal.
 *
 * @param category {@code daily} and {@code prayer} are habits that repeat; {@code task} and {@code goal} are one-off
 * @param completedLocalDate the date of the completion in {@code completionTimeZone}
 * @param recurrence how a habit repeats ({@code daily}, {@code weekdays}, {@code weekly}); null for one-off tasks
 * @param lastReset the last day a habit was reopened or completed; the daily reset never reopens it twice that day
 * @param prayerName the prayer a prayer habit tracks
 * @param workflowState a project-board column, for tasks on a project's board
 */
@Builder(toBuilder = true)
public record Task(
        String id,
        String title,
        String description,
        String category,
        String priority,
        boolean completed,
        Instant completedAt,
        LocalDate completedLocalDate,
        String completionTimeZone,
        LocalDate dueDate,
        String recurrence,
        LocalDate lastReset,
        String prayerName,
        String goalTag,
        String emoji,
        String projectId,
        String workflowState,
        String blockedReason,
        Integer boardOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static final String PRAYER = "prayer";
    public static final String DAILY = "daily";
    public static final String GOAL = "goal";
    public static final String ONE_OFF = "task";

    public boolean isHabit() {
        return DAILY.equals(category) || PRAYER.equals(category);
    }

    public boolean isPrayer() {
        return PRAYER.equals(category);
    }

    /** The task reopened: no completion. */
    public Task reopened(Instant now) {
        return toBuilder().completed(false).completedAt(null).completedLocalDate(null).completionTimeZone(null)
                .updatedAt(now).build();
    }
}
