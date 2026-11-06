package com.yourlifeone.finance.ledger.domain;

import java.util.List;

/** A saved transaction and the accounts whose balance it changed. */
public record TransactionSaved(Transaction transaction, List<Account> accounts) {
}
