package com.yourlifeone.finance.ledger.api.response;

import java.util.List;

/** @param accounts the accounts whose balance the save changed */
public record TransactionSavedResponse(TransactionResponse transaction, List<AccountResponse> accounts) {
}
