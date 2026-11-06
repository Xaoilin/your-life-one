package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One spending or income category's total in a month. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewCategoryAmount(
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @Min(Limits.MIN_SIGNED_PENCE) @Max(Limits.MAX_PENCE) Long amountPence) {
}
