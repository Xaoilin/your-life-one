package com.yourlifeone.finance.ledger.domain;

import java.util.List;

/**
 * Everything in the user's ledger.
 *
 * @param accounts by sort order, then oldest first
 * @param transactions newest date first, then newest created first
 */
public record Ledger(List<Account> accounts, List<Transaction> transactions, List<Budget> budgets,
        List<SavingsGoal> savingsGoals) {
}
