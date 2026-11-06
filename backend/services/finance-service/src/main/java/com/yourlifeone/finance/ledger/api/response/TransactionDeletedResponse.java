package com.yourlifeone.finance.ledger.api.response;

import java.util.List;

/** @param accounts the accounts whose balance the reversal changed */
public record TransactionDeletedResponse(String id, List<AccountResponse> accounts) {
}
