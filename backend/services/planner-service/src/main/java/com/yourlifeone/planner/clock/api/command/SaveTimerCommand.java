package com.yourlifeone.planner.clock.api.command;

import com.yourlifeone.planner.clock.api.request.TimerRequest;

/** Creates or replaces one timer. The queued payload carries the ID from the request path. */
public record SaveTimerCommand(String id, TimerRequest timer) {
}
