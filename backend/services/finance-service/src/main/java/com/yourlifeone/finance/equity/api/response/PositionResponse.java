package com.yourlifeone.finance.equity.api.response;

import com.yourlifeone.finance.equity.domain.EquityAction;
import com.yourlifeone.finance.equity.domain.EquityDetail;
import com.yourlifeone.finance.equity.domain.EquityGrant;
import com.yourlifeone.finance.equity.domain.EquityPlan;
import com.yourlifeone.finance.equity.domain.EquityScenario;
import com.yourlifeone.finance.equity.domain.EquitySource;
import java.time.Instant;
import java.util.List;

/** A saved position; its parts have the draft's shape, with unknown optional values as null. */
public record PositionResponse(String id, String company, String asOf, Long ownedShares, EquityPlan stockPlan,
        EquityPlan optionPlan, String employmentNote, List<EquityGrant> grants, List<EquityAction> actions,
        List<EquityDetail> details, List<EquitySource> sources, EquityScenario scenario, Instant createdAt,
        Instant updatedAt) {
}
