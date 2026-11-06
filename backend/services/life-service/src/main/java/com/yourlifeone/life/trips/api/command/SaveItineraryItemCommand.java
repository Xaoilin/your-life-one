package com.yourlifeone.life.trips.api.command;

import com.yourlifeone.life.trips.api.request.ItineraryItemRequest;

/** Creates or replaces one day plan; the queued payload carries the ID from the path. */
public record SaveItineraryItemCommand(String id, ItineraryItemRequest item) {
}
