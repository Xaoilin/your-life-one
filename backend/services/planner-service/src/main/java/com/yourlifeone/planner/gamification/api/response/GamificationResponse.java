package com.yourlifeone.planner.gamification.api.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** @param habitTallies completions per habit task ID */
public record GamificationResponse(
        long totalXp,
        int level,
        String title,
        int currentStreak,
        int longestStreak,
        LocalDate lastCompletionDate,
        int totalTasksCompleted,
        List<String> badges,
        Map<String, Integer> habitTallies) {
}
