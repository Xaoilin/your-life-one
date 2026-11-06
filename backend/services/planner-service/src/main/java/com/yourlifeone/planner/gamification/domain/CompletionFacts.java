package com.yourlifeone.planner.gamification.domain;

import lombok.Builder;

/**
 * What the badges look at besides the profile, counted when a completion is rewarded.
 *
 * @param completionsToday completions on the reward's day before this one
 * @param hourOfDay 0-23 in the app's time zone
 * @param dayOfWeek 0 for Sunday to 6 for Saturday
 * @param prayerHabitsCompleted prayer habits completed on the reward's day
 */
@Builder
public record CompletionFacts(
        int completionsToday,
        int hourOfDay,
        int dayOfWeek,
        int totalHabits,
        int totalGoalsCreated,
        int totalGoalsCompleted,
        int totalHighPriorityCompleted,
        boolean completedHabitToday,
        boolean completedTaskToday,
        boolean completedGoalToday,
        boolean allHabitsDoneToday,
        int goalCategories,
        long daysSinceFirstUse,
        boolean hadPriorStreak,
        KnowledgeProgress knowledge,
        int prayerHabitsCompleted) {
}
