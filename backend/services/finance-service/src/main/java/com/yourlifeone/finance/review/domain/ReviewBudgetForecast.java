package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourlifeone.finance.shared.json.WholeNumberDeserializer;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A saved monthly spending plan; the baseline month records which imported month started the plan. */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewBudgetForecast(
        @NotNull @Pattern(regexp = Limits.MONTH) String baselineMonth,
        @NotNull @Size(max = ReviewBudgetForecast.MAX_ITEMS) List<@NotNull @Valid ReviewBudgetForecastItem> items,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonDeserialize(using = WholeNumberDeserializer.class)
        @Min(0) @Max(Limits.MAX_PENCE) Long incomePence,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Size(max = Limits.MAX_TEXT) String incomeBasis) {
    public static final int MAX_ITEMS = 200;
}
