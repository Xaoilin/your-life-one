package com.yourlifeone.finance.equity.api.request;

import com.yourlifeone.finance.equity.domain.EquityPositionDraft;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creates or replaces a whole position.
 *
 * @param expectedUpdatedAt the exact {@code updatedAt} last read, or null to create the position
 */
public record SavePositionRequest(@NotNull @Valid EquityPositionDraft position,
        @Size(min = 1, max = Limits.MAX_VERSION) String expectedUpdatedAt) {
}
