package com.yourlifeone.finance.equity.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** A saved position: the draft plus its ID and times. {@code updatedAt} is its version. */
@Builder
public record EquityPosition(String id, String company, String asOf, Long ownedShares, EquityPlan stockPlan,
        EquityPlan optionPlan, String employmentNote, List<EquityGrant> grants, List<EquityAction> actions,
        List<EquityDetail> details, List<EquitySource> sources, EquityScenario scenario, Instant createdAt,
        Instant updatedAt) {
}
