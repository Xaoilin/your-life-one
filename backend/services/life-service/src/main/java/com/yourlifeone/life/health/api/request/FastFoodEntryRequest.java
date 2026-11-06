package com.yourlifeone.life.health.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * The whole entry; the ID is in the path. Allowed ratings and symptoms are checked against configuration.
 *
 * @param order null or blank when not recorded
 */
public record FastFoodEntryRequest(
        @NotBlank @Size(max = 160) String venue,
        @NotNull LocalDate date,
        @Size(max = 500) String order,
        @NotNull @Size(max = 32) String rating,
        @NotNull @Size(max = 20) List<@NotNull @Size(max = 32) String> symptoms,
        @Size(max = 4000) String notes) {
}
