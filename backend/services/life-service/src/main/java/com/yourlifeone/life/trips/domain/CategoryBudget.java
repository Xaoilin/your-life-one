package com.yourlifeone.life.trips.domain;

/**
 * One budget category's totals, in minor units.
 *
 * @param uncostedCount bookings in this category with no cost yet
 */
public record CategoryBudget(String category, long forecast, long paid, int count, int uncostedCount) {
}
