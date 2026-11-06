package com.yourlifeone.finance.equity.api.command;

/** Removes one position, only if it is still the version the caller read. */
public record DeletePositionCommand(String id, String expectedUpdatedAt) {
}
