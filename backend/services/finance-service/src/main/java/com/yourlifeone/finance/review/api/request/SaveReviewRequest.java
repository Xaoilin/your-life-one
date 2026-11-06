package com.yourlifeone.finance.review.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourlifeone.finance.review.domain.FinanceReviewDraft;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Replaces the whole banking review.
 *
 * @param expectedUpdatedAt the exact {@code updatedAt} last read, or null for the first save
 */
public record SaveReviewRequest(@Valid FinanceReviewDraft review,
        @Size(min = 1, max = Limits.MAX_VERSION) String expectedUpdatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Valid LoanProfileChangeRequest loanProfileChange) {
    @AssertTrue(message = "Supply either review or loanProfileChange; profile changes require expectedUpdatedAt.")
    @JsonIgnore
    public boolean isValidWrite() {
        return (review != null) != (loanProfileChange != null)
                && (loanProfileChange == null || expectedUpdatedAt != null);
    }
}
