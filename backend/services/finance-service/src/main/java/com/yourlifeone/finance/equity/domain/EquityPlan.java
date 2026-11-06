package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * What the user plans to do with their shares or their options.
 *
 * @param status {@code agreed}, {@code tentative} or {@code undecided}
 * @param reviewMonth when to look at it again ({@code YYYY-MM}), or null
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquityPlan(
        @NotNull @Size(max = Limits.MAX_TEXT) String summary,
        @NotNull @Pattern(regexp = "agreed|tentative|undecided") String status,
        @NotNull @Size(max = Limits.MAX_TEXT) String waitingFor,
        @NotNull @Size(max = Limits.MAX_TEXT) String nextAction,
        @Pattern(regexp = Limits.MONTH) String reviewMonth) {
}
