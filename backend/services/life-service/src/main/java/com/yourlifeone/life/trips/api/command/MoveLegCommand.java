package com.yourlifeone.life.trips.api.command;

/** Moves one leg a place earlier (-1) or later (1) in its route. */
public record MoveLegCommand(String id, int direction) {
}
