package com.yourlifeone.life.trips.api.command;

import com.yourlifeone.life.trips.api.request.LegRequest;

/** Creates or replaces one leg; the queued payload carries the ID from the path. */
public record SaveLegCommand(String id, LegRequest leg) {
}
