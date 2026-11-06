package com.yourlifeone.finance.ledger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalanceChangeTest {
    private static Transaction transaction(String type, long amount, String account, String to) {
        return Transaction.builder().id("t").type(type).amount(amount).accountId(account).toAccountId(to).build();
    }

    @Test
    void incomeAddsExpenseTakesAndATransferMovesBetweenAccounts() {
        var change = new BalanceChange()
                .apply(transaction(Transaction.INCOME, 500, "a", null))
                .apply(transaction(Transaction.EXPENSE, 200, "a", null))
                .apply(transaction(Transaction.TRANSFER, 100, "a", "b"));

        assertEquals(200, change.deltaFor("a"));
        assertEquals(100, change.deltaFor("b"));
    }

    @Test
    void replacingATransactionRevertsTheOldEffectBeforeApplyingTheNewOne() {
        var change = new BalanceChange()
                .revert(transaction(Transaction.TRANSFER, 1000, "a", "b"))
                .apply(transaction(Transaction.EXPENSE, 1000, "a", null));

        assertEquals(0, change.deltaFor("a"));
        assertEquals(-1000, change.deltaFor("b"));
        assertEquals(2, change.accountIds().size());
    }
}
