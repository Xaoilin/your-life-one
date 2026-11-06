package com.yourlifeone.finance.ledger.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

/**
 * Creates or replaces a savings goal. Amounts are integer pence; the service stamps when it was completed.
 *
 * @param linkedAccountId the account it is saved in, or null
 */
@Builder
public record SavingsGoalRequest(
        @NotBlank String name,
        @NotNull @Min(0) Long targetAmount,
        @NotNull @Min(0) Long currentAmount,
        String linkedAccountId,
        @Size(max = AccountRequest.MAX_STYLE) String icon,
        LocalDate deadline,
        @NotNull Boolean completed) {
}
