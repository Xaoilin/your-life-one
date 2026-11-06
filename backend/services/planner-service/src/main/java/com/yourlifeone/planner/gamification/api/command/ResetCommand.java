package com.yourlifeone.planner.gamification.api.command;

/**
 * Starts the user's progress again; it must say so, so a stray request can never wipe it.
 *
 * @param confirm must be true
 */
public record ResetCommand(boolean confirm) {
}
