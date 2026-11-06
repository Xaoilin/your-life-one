package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/** The monthly budget the review works out, in pence. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewBudget(
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long incomePence,
        @NotNull @Size(max = Limits.MAX_TEXT) String incomeBasis,
        @NotNull @Size(max = ReviewBudget.MAX_ESSENTIALS) List<@NotNull @Valid ReviewEssential> essentials,
        @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long workCostsPence,
        @NotNull @Size(max = Limits.MAX_TEXT) String workCostsNote,
        @NotNull @Size(max = ReviewBudget.MAX_SCENARIOS) List<@NotNull @Valid ReviewScenario> scenarios) {
    public static final int MAX_ESSENTIALS = 100;
    public static final int MAX_SCENARIOS = 50;
}
