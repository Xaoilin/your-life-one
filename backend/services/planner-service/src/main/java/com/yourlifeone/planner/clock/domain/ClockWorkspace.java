package com.yourlifeone.planner.clock.domain;

import java.util.List;

/**
 * Every stopwatch and timer, in the order they were added, with the numbers the next new ones are named after.
 */
public record ClockWorkspace(List<Stopwatch> stopwatches, List<CountdownTimer> timers, int nextStopwatchNumber,
        int nextTimerNumber) {
}
