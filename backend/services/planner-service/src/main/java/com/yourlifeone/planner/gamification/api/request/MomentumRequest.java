package com.yourlifeone.planner.gamification.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * A pillar's whole new state.
 *
 * @param expectedVersion the version this state was made from; null when the pillar has none yet
 */
public record MomentumRequest(@NotNull Map<String, Object> state, @Min(0) Integer expectedVersion) {
}
