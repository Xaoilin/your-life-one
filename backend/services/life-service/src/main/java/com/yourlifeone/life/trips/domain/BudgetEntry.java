package com.yourlifeone.life.trips.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/** A manual budget item; {@code amount} is in minor units. */
@Builder(toBuilder = true)
public record BudgetEntry(
        String id,
        String tripId,
        String title,
        String category,
        long amount,
        String status,
        LocalDate date,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
