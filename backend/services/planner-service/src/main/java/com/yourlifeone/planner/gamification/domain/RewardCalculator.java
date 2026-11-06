package com.yourlifeone.planner.gamification.domain;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Rewards one completion: the streak moves on to its day, XP is added with the streak's multiplier, the
 * level follows the XP, and any new badges are earned.
 */
public final class RewardCalculator {
    private RewardCalculator() {
    }

    /**
     * @param day the completion's date in the app's time zone
     * @param facts the counts the badges look at, taken before this completion
     */
    public static Rewarded reward(Profile profile, String priority, String category, LocalDate day,
            CompletionFacts facts) {
        var streaked = Streaks.afterCompletion(profile, day);
        var xp = XpRules.taskXp(priority, category, streaked.currentStreak());
        var totalXp = profile.totalXp() + xp;
        var level = Levels.levelFromXp(totalXp);
        var counted = streaked.toBuilder()
                .totalXp(totalXp)
                .level(level)
                .totalTasksCompleted(profile.totalTasksCompleted() + 1)
                .build();
        var newBadges = BadgeRules.newBadges(counted, category, priority, facts);
        var badges = new ArrayList<>(profile.badges());
        badges.addAll(newBadges);
        var after = counted.toBuilder().badges(badges).build();
        var reward = Reward.builder()
                .xpEarned(xp)
                .level(level)
                .leveledUp(level > profile.level())
                .title(Levels.titleForLevel(level))
                .newBadges(newBadges)
                .currentStreak(after.currentStreak())
                .streakMilestone(Streaks.isMilestone(after.currentStreak())
                        && after.currentStreak() != profile.currentStreak())
                .build();
        return new Rewarded(after, reward);
    }
}
