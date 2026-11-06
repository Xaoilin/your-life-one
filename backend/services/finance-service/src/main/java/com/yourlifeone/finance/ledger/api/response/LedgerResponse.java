package com.yourlifeone.finance.ledger.api.response;

import java.util.List;

public record LedgerResponse(List<AccountResponse> accounts, List<TransactionResponse> transactions,
        List<BudgetResponse> budgets, List<SavingsGoalResponse> savingsGoals) {
}
