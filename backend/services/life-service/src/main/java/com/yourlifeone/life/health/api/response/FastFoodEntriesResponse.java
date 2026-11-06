package com.yourlifeone.life.health.api.response;

import java.util.List;

/** Every journal entry, newest meal first. */
public record FastFoodEntriesResponse(List<FastFoodEntryResponse> entries) {
}
