package com.yourlifeone.life.trips.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record BudgetEntryResponse(String id, String tripId, String title, String category, long amount, String status,
        LocalDate date, String notes, Instant createdAt, Instant updatedAt) {
}
