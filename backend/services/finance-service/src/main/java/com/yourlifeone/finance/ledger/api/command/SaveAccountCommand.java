package com.yourlifeone.finance.ledger.api.command;

import com.yourlifeone.finance.ledger.api.request.AccountRequest;

/** Creates or replaces one record. The queued payload carries the ID from the request path. */
public record SaveAccountCommand(String id, AccountRequest request) {
}
