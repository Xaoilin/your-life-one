package com.yourlifeone.life.trips.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
        String id,
        String name,
        String summary,
        String notes,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String budgetCurrency,
        long budgetTotal,
        BudgetSummaryResponse budget,
        Instant createdAt,
        Instant updatedAt) {
}
