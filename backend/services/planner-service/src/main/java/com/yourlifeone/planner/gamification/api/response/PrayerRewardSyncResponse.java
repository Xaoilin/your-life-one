package com.yourlifeone.planner.gamification.api.response;

import com.yourlifeone.planner.tasks.api.response.TaskResponse;

/**
 * @param reward what the prayer asked about earned; null when it earned nothing
 * @param task that prayer's habit when this completed or reopened it; else null
 */
public record PrayerRewardSyncResponse(RewardResponse reward, TaskResponse task, GamificationResponse profile) {
}
