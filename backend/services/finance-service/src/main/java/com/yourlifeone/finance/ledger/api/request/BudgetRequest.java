package com.yourlifeone.finance.ledger.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Creates or replaces a monthly budget.
 *
 * @param category an expense category; each has at most one budget
 * @param monthlyLimit integer pence
 */
public record BudgetRequest(@NotBlank String category, @NotNull @Min(0) Long monthlyLimit) {
}
