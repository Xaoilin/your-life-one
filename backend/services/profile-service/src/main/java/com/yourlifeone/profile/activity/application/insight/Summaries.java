package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.SummaryCounts;
import com.yourlifeone.profile.activity.domain.UsageSummary;
import org.springframework.stereotype.Component;

/** The headline numbers: counts plus the failure rate among events that report an outcome. */
@Component
public class Summaries {
    public UsageSummary summarise(SummaryCounts counts) {
        return UsageSummary.builder()
                .eventCount(counts.events())
                .sessionCount(counts.sessions())
                .activeSurfaceCount(counts.surfaces())
                .errorCount(counts.errors())
                .failureRate(Percentages.of(counts.errors(), counts.outcomes()))
                .build();
    }
}
