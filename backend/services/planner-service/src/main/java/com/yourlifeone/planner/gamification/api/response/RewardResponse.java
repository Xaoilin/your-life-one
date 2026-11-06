package com.yourlifeone.planner.gamification.api.response;

import java.util.List;

/**
 * What one completion earned.
 *
 * @param newBadges IDs from the app's badge catalogue
 * @param streakMilestone the completion took the streak to 7, 14, 30, 60 or 100 days
 */
public record RewardResponse(
        int xpEarned,
        int level,
        boolean leveledUp,
        String title,
        List<String> newBadges,
        int currentStreak,
        boolean streakMilestone) {
}
