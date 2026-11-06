package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * One month's cash flow, in pence.
 *
 * @param netPence always income minus outflow
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewMonth(
        @NotNull @Pattern(regexp = Limits.MONTH) String month,
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long incomePence,
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long outflowPence,
        @NotNull @Min(Limits.MIN_SIGNED_PENCE) @Max(Limits.MAX_PENCE) Long netPence,
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long salaryPence,
        @NotNull @Size(max = ReviewMonth.MAX_CATEGORIES) List<@NotNull @Valid ReviewCategoryAmount> categories) {
    public static final int MAX_CATEGORIES = 100;
}
