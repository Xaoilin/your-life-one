package com.yourlifeone.planner.gamification.api.response;

import java.util.List;

/** Every pillar with a saved state. */
public record MomentumResponse(List<MomentumPillarResponse> pillars) {
}
