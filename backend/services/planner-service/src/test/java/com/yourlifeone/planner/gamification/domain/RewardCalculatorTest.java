package com.yourlifeone.planner.gamification.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The same XP, levels, streaks and badges the app used to work out itself. */
class RewardCalculatorTest {
    private static final LocalDate DAY = LocalDate.parse("2026-09-27");

    private static CompletionFacts facts(int completionsToday, int hour) {
        return CompletionFacts.builder().completionsToday(completionsToday).hourOfDay(hour).dayOfWeek(0)
                .knowledge(KnowledgeProgress.none()).build();
    }

    @Test
    void aFirstCompletionStartsTheStreakAndEarnsBaseXp() {
        var rewarded = RewardCalculator.reward(Profile.empty(), "medium", "task", DAY, facts(0, 12));

        assertEquals(10, rewarded.reward().xpEarned());
        assertEquals(1, rewarded.profile().currentStreak());
        assertEquals(DAY, rewarded.profile().lastCompletionDate());
        assertEquals(1, rewarded.profile().totalTasksCompleted());
        assertTrue(rewarded.reward().newBadges().containsAll(List.of("first-blood", "lunch-hustler", "weekend-warrior")));
    }

    @Test
    void habitsAndGoalsEarnBonusesMultipliedByTheStreak() {
        var onStreak = Profile.empty().toBuilder().currentStreak(13).longestStreak(13)
                .lastCompletionDate(DAY.minusDays(1)).build();

        // 20 (high) + 5 (habit) = 25, doubled at a 14-day streak.
        assertEquals(50, RewardCalculator.reward(onStreak, "high", "prayer", DAY, facts(0, 12)).reward().xpEarned());
        // 5 (low) + 50 (goal) = 55 at a 1-day streak.
        assertEquals(55, RewardCalculator.reward(Profile.empty(), "low", "goal", DAY, facts(0, 12)).reward().xpEarned());
    }

    @Test
    void aSecondCompletionTheSameDayKeepsTheStreak() {
        var today = Profile.empty().toBuilder().currentStreak(3).longestStreak(5).lastCompletionDate(DAY).build();

        var rewarded = RewardCalculator.reward(today, "medium", "task", DAY, facts(2, 12));

        assertEquals(3, rewarded.profile().currentStreak());
        assertFalse(rewarded.reward().streakMilestone());
        assertEquals(13, rewarded.reward().xpEarned());
    }

    @Test
    void levelsFollowXpAndABadgeIsNeverEarnedTwice() {
        var nearLevelTwo = new Profile(95, 1, 0, 0, null, 3, List.of("first-blood"), Map.of());

        var rewarded = RewardCalculator.reward(nearLevelTwo, "medium", "task", DAY, facts(0, 12));

        assertEquals(2, rewarded.reward().level());
        assertTrue(rewarded.reward().leveledUp());
        assertFalse(rewarded.reward().newBadges().contains("first-blood"));
        assertTrue(rewarded.reward().newBadges().containsAll(List.of("level-2", "xp-100")));
    }

    @Test
    void aStreakMilestoneIsReportedWhenReached() {
        var six = Profile.empty().toBuilder().currentStreak(6).longestStreak(6).lastCompletionDate(DAY.minusDays(1)).build();

        assertTrue(RewardCalculator.reward(six, "medium", "task", DAY, facts(0, 12)).reward().streakMilestone());
    }

    @Test
    void levelsAndTitles() {
        assertEquals(1, Levels.levelFromXp(0));
        assertEquals(21, Levels.levelFromXp(12021));
        assertEquals("Legend", Levels.titleForLevel(21));
        assertEquals("Focused", Levels.titleForLevel(5));
    }
}
