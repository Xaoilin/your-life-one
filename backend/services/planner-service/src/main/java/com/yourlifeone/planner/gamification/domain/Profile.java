package com.yourlifeone.planner.gamification.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * A user's progress: XP and the level it gives, the daily completion streak and earned badges.
 *
 * @param lastCompletionDate the app-time-zone date of the last rewarded completion, which the streak counts from
 * @param habitTallies completions per habit task ID
 */
@Builder(toBuilder = true)
public record Profile(
        long totalXp,
        int level,
        int currentStreak,
        int longestStreak,
        LocalDate lastCompletionDate,
        int totalTasksCompleted,
        List<String> badges,
        Map<String, Integer> habitTallies) {

    public static Profile empty() {
        return new Profile(0, 1, 0, 0, null, 0, List.of(), Map.of());
    }
}
