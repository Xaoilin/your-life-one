package com.yourlifeone.planner.gamification.application.service;

import com.yourlifeone.planner.gamification.domain.CompletionFacts;
import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.Profile;
import com.yourlifeone.planner.tasks.domain.Task;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Counts what the badges look at from the user's tasks as they were just before a completion. */
@Component
public class CompletionFactsFactory {
    private static final int DAYS_PER_WEEK = 7;

    /**
     * @param tasks every task before the completion
     * @param day the date the completion counts towards
     * @param at when it happened; its hour and weekday are read in {@code zone}
     */
    public CompletionFacts facts(List<Task> tasks, String completedTaskId, LocalDate day, Instant at, ZoneId zone,
            Profile profile, KnowledgeProgress knowledge) {
        var habits = tasks.stream().filter(Task::isHabit).toList();
        var goals = tasks.stream().filter(task -> Task.GOAL.equals(task.category())).toList();
        var doneToday = tasks.stream().filter(task -> task.completed() && day.equals(task.completedLocalDate())).toList();
        var local = at.atZone(zone);
        return CompletionFacts.builder()
                .completionsToday((int) doneToday.stream().filter(task -> !task.id().equals(completedTaskId)).count())
                .hourOfDay(local.getHour())
                .dayOfWeek(local.getDayOfWeek().getValue() % DAYS_PER_WEEK)
                .totalHabits(habits.size())
                .totalGoalsCreated(goals.size())
                .totalGoalsCompleted((int) goals.stream().filter(Task::completed).count())
                .totalHighPriorityCompleted((int) tasks.stream()
                        .filter(task -> task.completed() && "high".equals(task.priority())).count())
                .completedHabitToday(doneToday.stream().anyMatch(Task::isHabit))
                .completedTaskToday(doneToday.stream().anyMatch(task -> Task.ONE_OFF.equals(task.category())))
                .completedGoalToday(doneToday.stream().anyMatch(task -> Task.GOAL.equals(task.category())))
                .allHabitsDoneToday(!habits.isEmpty() && habits.stream().allMatch(Task::completed))
                .goalCategories((int) goals.stream().map(Task::goalTag).filter(Objects::nonNull).distinct().count())
                .daysSinceFirstUse(daysSinceFirstUse(tasks, at))
                .hadPriorStreak(profile.longestStreak() > profile.currentStreak())
                .knowledge(knowledge)
                .prayerHabitsCompleted((int) doneToday.stream().filter(Task::isPrayer).count())
                .build();
    }

    /** Whole days, rounded up, since the oldest task was created. */
    private static long daysSinceFirstUse(List<Task> tasks, Instant at) {
        return tasks.stream().map(Task::createdAt).filter(Objects::nonNull).min(Instant::compareTo)
                .map(first -> Math.max(0, ceilDays(Duration.between(first, at))))
                .orElse(0L);
    }

    private static long ceilDays(Duration duration) {
        var days = duration.toDays();
        return duration.minusDays(days).isZero() ? days : days + 1;
    }
}
