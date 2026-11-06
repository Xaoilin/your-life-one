package com.yourlifeone.planner.tasks.api.response;

import java.util.List;

/** Every task, oldest first. */
public record TasksResponse(List<TaskResponse> tasks) {
}
