package com.yourlifeone.planner.tasks.api.response;

import com.yourlifeone.planner.gamification.api.response.GamificationResponse;
import java.util.List;

/** Every task and the progress after the daily reset. */
public record DayResponse(List<TaskResponse> tasks, GamificationResponse profile) {
}
