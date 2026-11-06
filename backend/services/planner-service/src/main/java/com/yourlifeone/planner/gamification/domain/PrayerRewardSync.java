package com.yourlifeone.planner.gamification.domain;

import com.yourlifeone.planner.tasks.domain.Task;

/**
 * Prayer rewards after they were brought in line with the prayer service.
 *
 * @param reward what the prayer the app asked about earned, whenever it was granted; null if it earned nothing
 * @param task that prayer's habit, when this completed or reopened it; else null
 */
public record PrayerRewardSync(Reward reward, Task task, Profile profile) {
}
