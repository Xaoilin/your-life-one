package com.yourlifeone.planner.clock.api.response;

import java.util.List;

public record StopwatchResponse(String id, String label, long accumulatedMs, Long startedAt, List<Long> laps) {
}
