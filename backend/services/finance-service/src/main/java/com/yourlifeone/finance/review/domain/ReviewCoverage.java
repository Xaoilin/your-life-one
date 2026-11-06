package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * The period the review's figures cover.
 *
 * @param completeThrough the last month ({@code YYYY-MM}) with every transaction in; not after {@code to}
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewCoverage(
        @NotNull @IsoDate String from,
        @NotNull @IsoDate String to,
        @NotNull @Pattern(regexp = Limits.MONTH) String completeThrough,
        @NotNull @Min(0) @Max(Limits.MAX_COUNT) Long transactionCount,
        @NotNull @Min(0) @Max(Limits.MAX_COUNT) Long accountCount,
        @NotNull @Size(max = Limits.MAX_TEXT) String note) {
}
