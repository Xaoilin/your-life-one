package com.yourlifeone.finance.ledger.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The net change a write makes to account balances. Income adds its amount to its account, an expense
 * takes it away, and a transfer takes it from its account and adds it to the receiving one. Replacing a
 * transaction reverts the old effect and applies the new one; deleting it reverts its effect.
 */
public final class BalanceChange {
    private final Map<String, Long> deltas = new LinkedHashMap<>();

    public BalanceChange apply(Transaction transaction) {
        return add(transaction, 1);
    }

    public BalanceChange revert(Transaction transaction) {
        return add(transaction, -1);
    }

    /** Every account the change names, including those whose net change is zero. */
    public Set<String> accountIds() {
        return Set.copyOf(deltas.keySet());
    }

    /** The net change to one account's balance, in pence. */
    public long deltaFor(String accountId) {
        return deltas.getOrDefault(accountId, 0L);
    }

    private BalanceChange add(Transaction transaction, int direction) {
        var amount = Math.multiplyExact(transaction.amount(), direction);
        switch (transaction.type()) {
            case Transaction.INCOME -> move(transaction.accountId(), amount);
            case Transaction.EXPENSE -> move(transaction.accountId(), -amount);
            case Transaction.TRANSFER -> {
                move(transaction.accountId(), -amount);
                move(transaction.toAccountId(), amount);
            }
            default -> throw new IllegalArgumentException("Unknown transaction type " + transaction.type());
        }
        return this;
    }

    private void move(String accountId, long amount) {
        deltas.merge(accountId, amount, Math::addExact);
    }
}
