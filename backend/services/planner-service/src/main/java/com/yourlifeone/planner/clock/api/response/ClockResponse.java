package com.yourlifeone.planner.clock.api.response;

import java.util.List;

/** The whole workspace, in the order its stopwatches and timers were added. */
public record ClockResponse(List<StopwatchResponse> stopwatches, List<TimerResponse> timers, int nextStopwatchNumber,
        int nextTimerNumber) {
}
