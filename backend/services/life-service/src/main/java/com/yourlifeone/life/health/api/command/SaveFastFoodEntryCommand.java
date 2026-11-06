package com.yourlifeone.life.health.api.command;

import com.yourlifeone.life.health.api.request.FastFoodEntryRequest;

/** Creates or replaces one entry. The queued payload carries the ID from the request path. */
public record SaveFastFoodEntryCommand(String id, FastFoodEntryRequest entry) {
}
