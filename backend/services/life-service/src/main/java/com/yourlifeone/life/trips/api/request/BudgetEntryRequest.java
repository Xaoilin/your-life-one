package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One manual budget item; the ID is in the path. {@code amount} is in minor units. */
public record BudgetEntryRequest(
        @NotBlank @Size(max = 256) String tripId,
        @NotBlank @Size(max = 200) String title,
        @NotNull @Size(max = 32) String category,
        @NotNull @Min(0) Long amount,
        @NotNull @Size(max = 32) String status,
        LocalDate date,
        @Size(max = 4000) String notes) {
}
