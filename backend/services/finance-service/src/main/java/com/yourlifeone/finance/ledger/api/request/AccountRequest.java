package com.yourlifeone.finance.ledger.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Creates or replaces an account. The balance is in integer pence; saving an account sets it as given.
 *
 * @param type one of {@code app.finance.ledger.account-types}
 * @param currency an ISO 4217 code such as {@code GBP}
 */
@Builder
public record AccountRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotNull Long balance,
        @NotNull @Pattern(regexp = AccountRequest.CURRENCY) String currency,
        @Size(max = AccountRequest.MAX_STYLE) String color,
        @Size(max = AccountRequest.MAX_STYLE) String icon,
        @NotNull Boolean includeInNetWorth,
        @NotNull @Min(0) Integer sortOrder) {
    public static final String CURRENCY = "^[A-Z]{3}$";
    /** The longest colour or icon name. */
    public static final int MAX_STYLE = 64;
}
