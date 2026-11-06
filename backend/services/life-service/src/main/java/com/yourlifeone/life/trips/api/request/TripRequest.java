package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The trip's own fields; the ID is in the path. Its dates are refitted to its legs whenever they change.
 *
 * @param budgetCurrency ISO 4217 code; blank uses {@code app.life.trips.default-currency}
 * @param budgetTotal minor units; null means 0
 */
public record TripRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String summary,
        @Size(max = 10000) String notes,
        @NotNull @Size(max = 32) String status,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 3) String budgetCurrency,
        @Min(0) Long budgetTotal) {
}
