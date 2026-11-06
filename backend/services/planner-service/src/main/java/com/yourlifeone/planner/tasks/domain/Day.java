package com.yourlifeone.planner.tasks.domain;

import com.yourlifeone.planner.gamification.domain.Profile;
import java.util.List;

/** Every task and the progress after the daily reset. */
public record Day(List<Task> tasks, Profile profile) {
}
