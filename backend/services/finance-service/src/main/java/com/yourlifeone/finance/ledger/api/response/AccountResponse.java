package com.yourlifeone.finance.ledger.api.response;

import java.time.Instant;

public record AccountResponse(String id, String name, String type, long balance, String currency, String color,
        String icon, boolean includeInNetWorth, int sortOrder, Instant createdAt, Instant updatedAt) {
}
