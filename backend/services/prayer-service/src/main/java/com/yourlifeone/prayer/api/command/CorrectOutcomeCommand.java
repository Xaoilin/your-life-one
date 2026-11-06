package com.yourlifeone.prayer.api.command;

import lombok.Builder;

import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import java.util.UUID;

/** A status correction for one outcome, as queued. */
@Builder
public record CorrectOutcomeCommand(UUID outcomeId, OutcomeStatus status, String source, OutcomeTiming timing) {
}
