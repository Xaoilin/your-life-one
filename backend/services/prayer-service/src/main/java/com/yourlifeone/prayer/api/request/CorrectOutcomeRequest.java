package com.yourlifeone.prayer.api.request;

import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrectOutcomeRequest(@NotNull OutcomeStatus status, @Size(max = 32) String source, OutcomeTiming timing) {
}
