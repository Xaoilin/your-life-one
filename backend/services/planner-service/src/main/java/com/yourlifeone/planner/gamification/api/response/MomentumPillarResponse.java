package com.yourlifeone.planner.gamification.api.response;

import java.time.Instant;
import java.util.Map;

public record MomentumPillarResponse(String pillar, Map<String, Object> state, int version, Instant updatedAt) {
}
