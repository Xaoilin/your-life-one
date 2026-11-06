package com.yourlifeone.prayer.competition.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/** Shared projection: no account IDs, individual prayers, cycle information, location or notes. */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompetitionSummary(LocalDate from, LocalDate to, Instant asOf, Integer onTime, Integer late, Integer missed,
        Integer pointsEarned, Integer pointsAvailable, Boolean allPrayed) {
}
