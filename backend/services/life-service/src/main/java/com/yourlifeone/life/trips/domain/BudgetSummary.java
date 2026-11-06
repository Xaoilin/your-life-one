package com.yourlifeone.life.trips.domain;

import java.util.List;

/**
 * A trip's budget: costed bookings plus manual items, in minor units.
 *
 * @param remaining the trip's budget total minus the forecast; negative when over budget
 */
public record BudgetSummary(long forecastTotal, long paidTotal, long remaining, int uncostedBookingCount,
        List<CategoryBudget> byCategory) {
}
