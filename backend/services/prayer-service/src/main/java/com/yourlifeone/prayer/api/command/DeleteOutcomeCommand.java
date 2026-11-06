package com.yourlifeone.prayer.api.command;

import java.util.UUID;

/** Removal of one outcome, as queued. */
public record DeleteOutcomeCommand(UUID outcomeId) {
}
