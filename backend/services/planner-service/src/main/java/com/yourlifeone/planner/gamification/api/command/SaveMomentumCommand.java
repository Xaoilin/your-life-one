package com.yourlifeone.planner.gamification.api.command;

import com.yourlifeone.planner.gamification.api.request.MomentumRequest;

/** Replaces one pillar's state. The queued payload carries the pillar from the request path. */
public record SaveMomentumCommand(String pillar, MomentumRequest momentum) {
}
