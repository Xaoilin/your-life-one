package com.yourlifeone.finance.ledger.api.response;

import java.util.List;

public record AccountDeletedResponse(String id, List<String> removedTransactionIds) {
}
