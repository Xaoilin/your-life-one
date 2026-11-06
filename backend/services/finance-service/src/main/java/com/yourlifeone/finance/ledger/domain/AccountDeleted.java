package com.yourlifeone.finance.ledger.domain;

import java.util.List;

/** A deleted account's ID and the transactions removed with it (both sides of transfers). */
public record AccountDeleted(String id, List<String> removedTransactionIds) {
}
