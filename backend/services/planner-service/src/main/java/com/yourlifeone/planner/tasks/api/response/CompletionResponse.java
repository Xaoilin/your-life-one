package com.yourlifeone.planner.tasks.api.response;

import com.yourlifeone.planner.gamification.api.response.GamificationResponse;
import com.yourlifeone.planner.gamification.api.response.RewardResponse;

/** @param reward null when the completion earned nothing (a habit already rewarded that day) */
public record CompletionResponse(TaskResponse task, RewardResponse reward, GamificationResponse profile) {
}
