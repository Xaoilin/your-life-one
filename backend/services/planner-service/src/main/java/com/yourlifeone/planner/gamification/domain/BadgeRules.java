package com.yourlifeone.planner.gamification.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Which badges a rewarded completion earns. The IDs and rules match the app's badge catalogue, which
 * holds their names and pictures; a badge is earned once and never taken back.
 */
public final class BadgeRules {
    private BadgeRules() {
    }

    /**
     * @param profile the profile after the completion's XP and streak
     * @param category the completed task's category
     * @param priority the completed task's priority
     */
    public static List<String> newBadges(Profile profile, String category, String priority, CompletionFacts facts) {
        var check = new Check(profile);
        earlyGame(check, profile, priority, facts);
        midGame(check, profile, category, facts);
        lateGame(check, profile, facts);
        endGame(check, profile, facts);
        islamic(check, facts);
        return check.earned;
    }

    private static void earlyGame(Check check, Profile profile, String priority, CompletionFacts facts) {
        var tasks = profile.totalTasksCompleted();
        var streak = profile.currentStreak();
        var today = facts.completionsToday() + 1;
        check.award("first-blood", tasks >= 1);
        check.award("getting-started", tasks >= 3);
        check.award("on-a-roll", tasks >= 5);
        check.award("task-10", tasks >= 10);
        check.award("task-25", tasks >= 25);
        check.award("streak-2", streak >= 2);
        check.award("streak-3", streak >= 3);
        check.award("streak-5", streak >= 5);
        check.award("hat-trick", today >= 3);
        check.award("busy-day", today >= 5);
        check.award("early-bird", facts.hourOfDay() < 9);
        check.award("night-owl", facts.hourOfDay() >= 22);
        check.award("habit-starter", facts.totalHabits() >= 1);
        check.award("routine-builder", facts.totalHabits() >= 3);
        check.award("all-habits", facts.allHabitsDoneToday() && facts.totalHabits() >= 1);
        check.award("dreamer", facts.totalGoalsCreated() >= 1);
        check.award("goal-setter", facts.totalGoalsCreated() >= 3);
        check.award("level-2", profile.level() >= 2);
        check.award("level-3", profile.level() >= 3);
        check.award("level-5", profile.level() >= 5);
        check.award("xp-50", profile.totalXp() >= 50);
        check.award("xp-100", profile.totalXp() >= 100);
        check.award("xp-250", profile.totalXp() >= 250);
        check.award("high-priority", "high".equals(priority) || facts.totalHighPriorityCompleted() >= 1);
        check.award("all-priorities", facts.totalHighPriorityCompleted() >= 1);
        check.award("multitasker", facts.completedHabitToday() && facts.completedTaskToday() && facts.completedGoalToday());
        check.award("weekend-warrior", facts.dayOfWeek() == 0 || facts.dayOfWeek() == 6);
        check.award("monday-motivation", facts.dayOfWeek() == 1);
        check.award("five-in-a-row", today >= 5);
        check.award("first-week", facts.daysSinceFirstUse() >= 7);
    }

    private static void midGame(Check check, Profile profile, String category, CompletionFacts facts) {
        var tasks = profile.totalTasksCompleted();
        var streak = profile.currentStreak();
        var today = facts.completionsToday() + 1;
        check.award("task-50", tasks >= 50);
        check.award("century", tasks >= 100);
        check.award("task-200", tasks >= 200);
        check.award("task-500", tasks >= 500);
        check.award("streak-7", streak >= 7);
        check.award("streak-10", streak >= 10);
        check.award("streak-14", streak >= 14);
        check.award("streak-21", streak >= 21);
        check.award("power-day", today >= 7);
        check.award("ten-a-day", today >= 10);
        check.award("goal-getter", "goal".equals(category) || facts.totalGoalsCompleted() >= 1);
        check.award("goal-3", facts.totalGoalsCompleted() >= 3);
        check.award("goal-5", facts.totalGoalsCompleted() >= 5);
        check.award("habits-5", facts.totalHabits() >= 5);
        check.award("perfect-week", streak >= 7 && facts.allHabitsDoneToday());
        check.award("habit-streak-14", streak >= 14 && facts.allHabitsDoneToday());
        check.award("level-7", profile.level() >= 7);
        check.award("level-10", profile.level() >= 10);
        check.award("xp-500", profile.totalXp() >= 500);
        check.award("xp-1000", profile.totalXp() >= 1000);
        check.award("xp-2500", profile.totalXp() >= 2500);
        check.award("early-week", facts.hourOfDay() < 7);
        check.award("lunch-hustler", facts.hourOfDay() == 12);
        check.award("comeback-kid", facts.hadPriorStreak() && streak >= 1);
        check.award("badge-10", check.total() >= 10);
        check.award("badge-25", check.total() >= 25);
        check.award("categories-3", facts.goalCategories() >= 3);
        check.award("full-day", facts.completedHabitToday() && facts.completedTaskToday());
        check.award("first-month", facts.daysSinceFirstUse() >= 30);
    }

    private static void lateGame(Check check, Profile profile, CompletionFacts facts) {
        var tasks = profile.totalTasksCompleted();
        var streak = profile.currentStreak();
        check.award("task-1000", tasks >= 1000);
        check.award("task-2500", tasks >= 2500);
        check.award("streak-30", streak >= 30);
        check.award("streak-60", streak >= 60);
        check.award("streak-90", streak >= 90);
        check.award("goal-10", facts.totalGoalsCompleted() >= 10);
        check.award("goal-25", facts.totalGoalsCompleted() >= 25);
        check.award("power-15", facts.completionsToday() + 1 >= 15);
        check.award("level-15", profile.level() >= 15);
        check.award("level-20", profile.level() >= 20);
        check.award("xp-5000", profile.totalXp() >= 5000);
        check.award("xp-10000", profile.totalXp() >= 10000);
        check.award("habits-10", facts.totalHabits() >= 10);
        check.award("perfect-month", streak >= 30 && facts.allHabitsDoneToday());
        check.award("badge-50", check.total() >= 50);
        check.award("multiplier-max", streak >= 14);
        check.award("categories-5", facts.goalCategories() >= 5);
        check.award("weekday-streak-20", streak >= 20);
        check.award("high-priority-10", facts.totalHighPriorityCompleted() >= 10);
        check.award("high-priority-50", facts.totalHighPriorityCompleted() >= 50);
        check.award("three-months", facts.daysSinceFirstUse() >= 90);
        check.award("night-shift", facts.hourOfDay() < 4);
        check.award("dawn-warrior", facts.hourOfDay() < 6);
    }

    private static void endGame(Check check, Profile profile, CompletionFacts facts) {
        var tasks = profile.totalTasksCompleted();
        var streak = profile.currentStreak();
        check.award("task-5000", tasks >= 5000);
        check.award("task-10000", tasks >= 10000);
        check.award("streak-100", streak >= 100);
        check.award("streak-200", streak >= 200);
        check.award("streak-365", streak >= 365);
        check.award("level-30", profile.level() >= 30);
        check.award("level-50", profile.level() >= 50);
        check.award("xp-25000", profile.totalXp() >= 25000);
        check.award("xp-100000", profile.totalXp() >= 100000);
        check.award("goal-50", facts.totalGoalsCompleted() >= 50);
        check.award("goal-100", facts.totalGoalsCompleted() >= 100);
        check.award("badge-75", check.total() >= 75);
        check.award("badge-100", check.total() >= 100);
        check.award("year-member", facts.daysSinceFirstUse() >= 365);
        check.award("perfect-quarter", streak >= 90 && facts.allHabitsDoneToday());
        check.award("streak-500", streak >= 500);
        check.award("xp-50000", profile.totalXp() >= 50000);
    }

    private static void islamic(Check check, CompletionFacts facts) {
        var knowledge = facts.knowledge() == null ? KnowledgeProgress.none() : facts.knowledge();
        var entries = knowledge.knowledgeEntries();
        var topics = knowledge.knowledgeTopics();
        check.award("first-note", entries >= 1);
        check.award("notes-5", entries >= 5);
        check.award("notes-10", entries >= 10);
        check.award("notes-25", entries >= 25);
        check.award("notes-50", entries >= 50);
        check.award("notes-100", entries >= 100);
        check.award("notes-250", entries >= 250);
        check.award("notes-500", entries >= 500);
        check.award("first-topic", topics >= 1);
        check.award("topics-3", topics >= 3);
        check.award("topics-5", topics >= 5);
        check.award("topics-10", topics >= 10);
        check.award("topics-20", topics >= 20);
        check.award("first-prayer-habit", facts.prayerHabitsCompleted() >= 1);
        check.award("five-prayers", facts.prayerHabitsCompleted() >= 5);
        check.award("lifestyle-first", knowledge.lifestyleTotal() >= 1);
        check.award("lifestyle-10", knowledge.lifestyleTotal() >= 10);
        check.award("haram-avoid-1", knowledge.lifestyleHaramMastered() >= 1);
        check.award("haram-avoid-3", knowledge.lifestyleHaramMastered() >= 3);
        check.award("haram-avoid-5", knowledge.lifestyleHaramMastered() >= 5);
        check.award("haram-avoid-10", knowledge.lifestyleHaramMastered() >= 10);
        check.award("halal-consist-1", knowledge.lifestyleHalalConsistent() >= 1);
        check.award("halal-consist-3", knowledge.lifestyleHalalConsistent() >= 3);
        check.award("halal-consist-5", knowledge.lifestyleHalalConsistent() >= 5);
        check.award("halal-consist-10", knowledge.lifestyleHalalConsistent() >= 10);
    }

    /** Collects badges not yet held, in rule order. */
    private static final class Check {
        private final HashSet<String> held;
        private final List<String> earned = new ArrayList<>();

        Check(Profile profile) {
            held = new HashSet<>(profile.badges());
        }

        void award(String id, boolean condition) {
            if (condition && !held.contains(id)) {
                held.add(id);
                earned.add(id);
            }
        }

        /** Badges held counting the ones earned so far by this completion. */
        int total() {
            return held.size();
        }
    }
}
