package com.yourlifeone.planner.clock.api.command;

import com.yourlifeone.planner.clock.api.request.StopwatchRequest;

/** Creates or replaces one stopwatch. The queued payload carries the ID from the request path. */
public record SaveStopwatchCommand(String id, StopwatchRequest stopwatch) {
}
