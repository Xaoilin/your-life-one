package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** An account-owned category classification, applied by label across the review's months. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewExpenseGroup(
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @Pattern(regexp = ReviewExpenseGroup.GROUP_PATTERN) String group) {
    public static final String GROUP_PATTERN = "non-discretionary|necessary|discretionary";
}
