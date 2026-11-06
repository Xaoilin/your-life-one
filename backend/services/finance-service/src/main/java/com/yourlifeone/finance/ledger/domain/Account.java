package com.yourlifeone.finance.ledger.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A manual account. Its balance (integer pence) changes with its transactions; the app may also set it.
 *
 * @param type one of {@code app.finance.ledger.account-types}
 * @param sortOrder where it is listed, lowest first
 */
@Builder(toBuilder = true)
public record Account(String id, String name, String type, long balance, String currency, String color, String icon,
        boolean includeInNetWorth, int sortOrder, Instant createdAt, Instant updatedAt) {
}
