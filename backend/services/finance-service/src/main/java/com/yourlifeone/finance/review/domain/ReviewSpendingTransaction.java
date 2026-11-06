package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourlifeone.finance.shared.json.WholeNumberDeserializer;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A dated personal payment or credit; month-only user confirmations retain their date uncertainty. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewSpendingTransaction(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = Limits.MAX_ID) String accountId,
        @IsoDate String date,
        @NotNull @Pattern(regexp = Limits.MONTH) String month,
        @NotBlank @Size(max = Limits.MAX_TEXT) String merchant,
        @NotNull @Size(max = Limits.MAX_TEXT) String description,
        @NotBlank @Size(max = Limits.MAX_TEXT) String category,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull
        @Min(Limits.MIN_SIGNED_PENCE) @Max(Limits.MAX_PENCE) Long amountPence,
        @Size(max = Limits.MAX_TEXT) String detail,
        @Pattern(regexp = ReviewSpendingTransaction.RECURRENCE_PATTERN) String recurrence,
        @Size(max = ReviewSpendingTransaction.MAX_SOURCES) List<@NotBlank @Size(max = Limits.MAX_ID) String> sourceIds,
        @NotNull @Pattern(regexp = ReviewSpendingTransaction.ORIGIN_PATTERN) String origin) {
    public static final String ORIGIN_PATTERN = "bank|user";
    public static final String RECURRENCE_PATTERN = "recurring|one-off|unknown";
    public static final int MAX_SOURCES = 100;
}
