package com.yourlifeone.planner.gamification.domain;

/**
 * XP for one completion: a base by priority, a bonus for habits and goals, multiplied by the streak.
 * Categories and priorities are the app's ({@code daily}, {@code prayer}, {@code task}, {@code goal};
 * {@code low}, {@code medium}, {@code high}).
 */
public final class XpRules {
    private static final int LOW = 5;
    private static final int MEDIUM = 10;
    private static final int HIGH = 20;
    private static final int HABIT_BONUS = 5;
    private static final int GOAL_BONUS = 50;

    private XpRules() {
    }

    public static int taskXp(String priority, String category, int currentStreak) {
        var xp = basePoints(priority);
        if (isHabit(category)) xp += HABIT_BONUS;
        if ("goal".equals(category)) xp += GOAL_BONUS;
        return (int) Math.round(xp * streakMultiplier(currentStreak));
    }

    public static double streakMultiplier(int streak) {
        if (streak >= 14) return 2.0;
        if (streak >= 7) return 1.5;
        if (streak >= 3) return 1.25;
        return 1.0;
    }

    /** Daily and prayer tasks are habits: they repeat, and earn XP at most once a day. */
    public static boolean isHabit(String category) {
        return "daily".equals(category) || "prayer".equals(category);
    }

    private static int basePoints(String priority) {
        return switch (priority) {
            case "high" -> HIGH;
            case "low" -> LOW;
            default -> MEDIUM;
        };
    }
}
