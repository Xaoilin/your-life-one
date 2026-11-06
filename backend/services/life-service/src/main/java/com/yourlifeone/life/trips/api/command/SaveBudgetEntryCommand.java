package com.yourlifeone.life.trips.api.command;

import com.yourlifeone.life.trips.api.request.BudgetEntryRequest;

/** Creates or replaces one budget item; the queued payload carries the ID from the path. */
public record SaveBudgetEntryCommand(String id, BudgetEntryRequest entry) {
}
