package com.yourlifeone.finance.review.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yourlifeone.finance.review.domain.LoanOverpaymentProfile;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Deterministic, replayable profile command; the service preserves every other review field. */
public record LoanProfileChangeRequest(
        @NotNull @Pattern(regexp = "save|remove") String operation,
        @Valid LoanOverpaymentProfile profile,
        @Size(min = 1, max = Limits.MAX_ID) String profileId,
        Boolean confirmed) {
    @AssertTrue(message = "Save requires only a profile; removal requires only a profile ID and confirmed=true.")
    @JsonIgnore
    public boolean isValidChange() {
        return "save".equals(operation) ? profile != null && profileId == null && confirmed == null
                : "remove".equals(operation) && profile == null && profileId != null
                        && !profileId.isBlank() && Boolean.TRUE.equals(confirmed);
    }
}
