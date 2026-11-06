package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.SummaryCounts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SummariesTest {
    private final Summaries summaries = new Summaries();

    @Test
    void failureRateIsErrorsAmongEventsThatReportAnOutcome() {
        var summary = summaries.summarise(SummaryCounts.builder()
                .events(40).sessions(4).surfaces(3).errors(1).outcomes(8).build());

        assertEquals(40, summary.eventCount());
        assertEquals(4, summary.sessionCount());
        assertEquals(3, summary.activeSurfaceCount());
        // 1 / 8 = 12.5%, rounded half up as the React app did.
        assertEquals(13, summary.failureRate());
    }

    @Test
    void hasNoFailureRateWithoutOutcomes() {
        assertNull(summaries.summarise(SummaryCounts.builder().events(5).errors(2).build()).failureRate());
    }
}
