package com.yourlifeone.planner.tasks.domain;

import com.yourlifeone.planner.gamification.domain.Profile;
import com.yourlifeone.planner.gamification.domain.Reward;

/**
 * A completed task with what it earned.
 *
 * @param reward null when it earned nothing (a habit already rewarded that day)
 */
public record Completion(Task task, Reward reward, Profile profile) {
}
