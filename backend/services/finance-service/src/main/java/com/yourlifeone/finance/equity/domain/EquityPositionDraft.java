package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * A private, dated stock and option planning record as the app or an AI agent writes it. Owned shares are
 * kept apart from unexercised grants; nothing here is a trade or a live quote.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquityPositionDraft(
        @NotBlank @Size(max = Limits.MAX_COMPANY) String company,
        @NotNull @IsoDate String asOf,
        @NotNull @Min(0) @Max(Limits.MAX_QUANTITY) Long ownedShares,
        @NotNull @Valid EquityPlan stockPlan,
        @NotNull @Valid EquityPlan optionPlan,
        @NotNull @Size(max = Limits.MAX_LONG_TEXT) String employmentNote,
        @NotNull @Size(max = EquityPositionDraft.MAX_ENTRIES) List<@NotNull @Valid EquityGrant> grants,
        @NotNull @Size(max = EquityPositionDraft.MAX_ENTRIES) List<@NotNull @Valid EquityAction> actions,
        @NotNull @Size(max = EquityPositionDraft.MAX_ENTRIES) List<@NotNull @Valid EquityDetail> details,
        @NotNull @Size(max = EquityPositionDraft.MAX_ENTRIES) List<@NotNull @Valid EquitySource> sources,
        @NotNull @Valid EquityScenario scenario) {
    public static final int MAX_ENTRIES = 100;
}
