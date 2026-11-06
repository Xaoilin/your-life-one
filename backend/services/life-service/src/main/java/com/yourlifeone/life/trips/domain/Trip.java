package com.yourlifeone.life.trips.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/**
 * A trip. Its dates follow its dated legs: the first leg's start and the last leg's end, in route order.
 *
 * @param budgetTotal the planned budget in minor units (pence, cents)
 */
@Builder(toBuilder = true)
public record Trip(
        String id,
        String name,
        String summary,
        String notes,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String budgetCurrency,
        long budgetTotal,
        Instant createdAt,
        Instant updatedAt) {
}
