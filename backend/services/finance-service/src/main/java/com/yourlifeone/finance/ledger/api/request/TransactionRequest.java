package com.yourlifeone.finance.ledger.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Creates or replaces a transaction; the service applies its effect on account balances.
 *
 * @param amount integer pence, more than zero
 * @param toAccountId the receiving account; required for a transfer and absent otherwise
 * @param date the day it happened, {@code YYYY-MM-DD}
 */
@Builder
public record TransactionRequest(
        @NotBlank String type,
        @NotNull @Positive Long amount,
        @NotBlank String category,
        @NotBlank String accountId,
        String toAccountId,
        String description,
        @NotNull LocalDate date,
        List<String> tags) {
}
