package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A monthly essential cost in the budget, in pence. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewEssential(
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long amountPence,
        @NotNull @Size(max = Limits.MAX_TEXT) String note) {
}
