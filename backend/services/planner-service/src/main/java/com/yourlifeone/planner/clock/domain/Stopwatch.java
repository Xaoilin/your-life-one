package com.yourlifeone.planner.clock.domain;

import java.util.List;
import lombok.Builder;

/**
 * A stopwatch. Times are epoch milliseconds, as the app's clock keeps them.
 *
 * @param accumulatedMs time counted before the current run
 * @param startedAt when the current run started; null while stopped
 * @param laps lap times in milliseconds, in the order recorded
 */
@Builder(toBuilder = true)
public record Stopwatch(String id, String label, long accumulatedMs, Long startedAt, List<Long> laps) {
}
