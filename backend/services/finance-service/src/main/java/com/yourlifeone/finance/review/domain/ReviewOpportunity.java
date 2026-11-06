package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * A place spending could come down, in pence a month.
 *
 * @param suggestedCapPence a suggested monthly cap, or null
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewOpportunity(
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long monthlyPence,
        @NotNull @Size(max = Limits.MAX_TEXT) String note,
        @Min(0) @Max(Limits.MAX_PENCE) Long suggestedCapPence) {
}
