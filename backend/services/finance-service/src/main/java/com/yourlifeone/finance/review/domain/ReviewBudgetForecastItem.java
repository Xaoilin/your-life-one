package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.json.WholeNumberDeserializer;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonDeserialize;

/** One editable monthly forecast allowance, in integer pence rather than an actual bank payment. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewBudgetForecastItem(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long amountPence,
        @NotNull @Pattern(regexp = ReviewExpenseGroup.GROUP_PATTERN) String group) {
}
