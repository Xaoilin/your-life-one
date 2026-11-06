package com.yourlifeone.life.trips.api.command;

import com.yourlifeone.life.trips.api.request.TripRequest;

/** Creates or replaces a trip's own fields; the queued payload carries the ID from the path. */
public record SaveTripCommand(String id, TripRequest trip) {
}
