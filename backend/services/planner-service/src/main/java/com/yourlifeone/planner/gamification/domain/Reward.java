package com.yourlifeone.planner.gamification.domain;

import java.util.List;
import lombok.Builder;

/**
 * What one rewarded completion earned, for the app's celebration.
 *
 * @param newBadges IDs of the badges earned by this completion
 * @param streakMilestone true when this completion took the streak to a milestone (7, 14, 30, 60 or 100 days)
 */
@Builder
public record Reward(
        int xpEarned,
        int level,
        boolean leveledUp,
        String title,
        List<String> newBadges,
        int currentStreak,
        boolean streakMilestone) {
}
