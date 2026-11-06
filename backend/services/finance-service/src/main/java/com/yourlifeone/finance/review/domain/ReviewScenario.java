package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * A planned or confirmed change to the monthly budget.
 *
 * @param status {@code planned} or {@code confirmed}
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewScenario(
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @Pattern(regexp = "planned|confirmed") String status,
        @NotNull @Min(Limits.MIN_SIGNED_PENCE) @Max(Limits.MAX_PENCE) Long monthlyAdjustmentPence,
        @NotNull @Size(max = Limits.MAX_TEXT) String note) {
}
