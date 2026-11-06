package com.yourlifeone.planner.tasks.api.command;

import com.yourlifeone.planner.tasks.api.request.TaskRequest;

/** Creates or replaces one task. The queued payload carries the ID from the request path. */
public record SaveTaskCommand(String id, TaskRequest task) {
}
