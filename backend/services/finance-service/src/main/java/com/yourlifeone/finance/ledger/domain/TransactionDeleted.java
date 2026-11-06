package com.yourlifeone.finance.ledger.domain;

import java.util.List;

/** A deleted transaction's ID and the accounts whose balance its reversal changed. */
public record TransactionDeleted(String id, List<Account> accounts) {
}
