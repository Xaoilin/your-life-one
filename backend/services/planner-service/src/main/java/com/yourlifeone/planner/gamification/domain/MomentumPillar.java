package com.yourlifeone.planner.gamification.domain;

import java.time.Instant;
import java.util.Map;

/**
 * One daily-momentum pillar's state, as the app keeps it (paths, progress logs, reminder preferences).
 *
 * @param version increases with every save; a save must name the version it changed
 */
public record MomentumPillar(String pillar, Map<String, Object> state, int version, Instant updatedAt) {
}
