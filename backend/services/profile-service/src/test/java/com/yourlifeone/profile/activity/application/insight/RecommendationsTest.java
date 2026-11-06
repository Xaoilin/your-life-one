package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.ErrorSummary;
import com.yourlifeone.profile.activity.domain.FunnelStage;
import com.yourlifeone.profile.activity.domain.Recommendation;
import com.yourlifeone.profile.activity.domain.SessionProgress;
import com.yourlifeone.profile.activity.domain.UsageSummary;
import com.yourlifeone.profile.activity.domain.UsageTrend;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationsTest {
    private final Recommendations recommendations = new Recommendations(InsightTestProperties.DEFAULTS);
    private final Funnel funnel = new Funnel();

    private static UsageSummary summary(long events, long sessions, long errors, Integer failureRate) {
        return UsageSummary.builder().eventCount(events).sessionCount(sessions).activeSurfaceCount(1)
                .errorCount(errors).failureRate(failureRate).build();
    }

    private static UsageTrend trend(String feature, String action, long current, long previous, Integer change) {
        return UsageTrend.builder().key(feature + ":" + action).feature(feature).action(action)
                .currentCount(current).previousCount(previous).changePercent(change).build();
    }

    private List<FunnelStage> healthyFunnel() {
        return funnel.stages(new SessionProgress(3, 3, 3));
    }

    @Test
    void drawsNoConclusionsBeforeTenEventsAcrossThreeSessions() {
        assertTrue(recommendations.isColdStart(summary(9, 3, 0, null)));
        assertTrue(recommendations.isColdStart(summary(40, 2, 0, null)));
        assertFalse(recommendations.isColdStart(summary(10, 3, 0, null)));
        assertEquals(List.of(), recommendations.recommend(summary(9, 3, 9, 100), List.of(), List.of(),
                List.of(new ErrorSummary("render_failed", 9, "dashboard"))));
    }

    @Test
    void recommendsAReliabilityReviewAtThreeErrorsAndATwentyPercentFailureRate() {
        var errors = List.of(new ErrorSummary("surface_unavailable", 4, "dashboard"));

        var result = recommendations.recommend(summary(15, 3, 4, 100), List.of(), healthyFunnel(), errors);

        assertEquals(List.of(Recommendation.builder()
                .id("reliability-review")
                .title("Review the highest-frequency failure path")
                .summary("Failures are a meaningful share of the selected activity window.")
                .evidence("4 error events across 15 events (100% failure outcomes). The most frequent code is"
                        + " surface unavailable.")
                .confidence("medium")
                .suggestedAction("Suggested action: inspect the affected surface and reproduce the coded failure before"
                        + " changing product behavior.")
                .build()), result);
        assertEquals(List.of(), recommendations.recommend(summary(15, 3, 4, 19), List.of(), healthyFunnel(), errors));
        assertEquals(List.of(), recommendations.recommend(summary(15, 3, 2, 100), List.of(), healthyFunnel(), errors));
    }

    @Test
    void recommendsInvestigatingADropOffWhenUnderSeventyPercentOfViewingSessionsOpen() {
        var dropOff = funnel.stages(new SessionProgress(10, 10, 6));

        var result = recommendations.recommend(summary(40, 10, 0, null), List.of(), dropOff, List.of());

        assertEquals(List.of(Recommendation.builder()
                .id("funnel-drop-off")
                .title("Investigate the surface-open drop-off")
                .summary("Fewer sessions reached a successful surface open than viewed a surface.")
                .evidence("6 of 10 viewed sessions reached a successful open (60% of started sessions).")
                .confidence("medium")
                .suggestedAction("Suggested action: compare the affected loading and error states with the intended entry"
                        + " flow; no automatic rearrangement is applied.")
                .build()), result);
        assertEquals(List.of(), recommendations.recommend(summary(40, 10, 0, null), List.of(),
                funnel.stages(new SessionProgress(10, 10, 7)), List.of()));
        assertEquals(List.of(), recommendations.recommend(summary(40, 10, 0, null), List.of(),
                funnel.stages(new SessionProgress(10, 4, 0)), List.of()));
    }

    @Test
    void recommendsReviewingTheFirstPathThatRoseByHalfAgainOrMore() {
        var trends = List.of(
                trend("navigation", "surface_viewed", 40, 30, 33),
                trend("calendar_sync", "run_started", 6, 4, 50),
                trend("tasks", "created", 9, 2, 350));

        var result = recommendations.recommend(summary(12, 3, 0, null), trends, healthyFunnel(), List.of());

        assertEquals(List.of(Recommendation.builder()
                .id("rising-calendar_sync:run_started")
                .title("Review the rising calendar sync path")
                .summary("This stable feature/action path appeared more often than in the preceding window.")
                .evidence("calendar sync / run started occurred 6 times versus 4 previously (50% change).")
                .confidence("medium")
                .suggestedAction("Suggested action: decide whether this usage pattern deserves product review; usage alone"
                        + " does not trigger a change.")
                .build()), result);
    }

    @Test
    void ordersReliabilityThenDropOffThenRisingAndRatesConfidenceBySampleSize() {
        var result = recommendations.recommend(summary(30, 9, 5, 50),
                List.of(trend("tasks", "created", 5, 2, 150)),
                funnel.stages(new SessionProgress(9, 9, 1)),
                List.of(new ErrorSummary("timeout", 5, "tasks")));

        assertEquals(List.of("reliability-review", "funnel-drop-off", "rising-tasks:created"),
                result.stream().map(Recommendation::id).toList());
        assertEquals(List.of("high", "low", "high"), result.stream().map(Recommendation::confidence).toList());
    }
}
