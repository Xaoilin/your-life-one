package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.config.ActivityInsightsProperties;
import com.yourlifeone.profile.activity.domain.ErrorSummary;
import com.yourlifeone.profile.activity.domain.FunnelStage;
import com.yourlifeone.profile.activity.domain.Recommendation;
import com.yourlifeone.profile.activity.domain.UsageSummary;
import com.yourlifeone.profile.activity.domain.UsageTrend;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Evidence-led review suggestions, at most three, in this order: a failure path worth reviewing, a
 * drop-off between viewing and opening surfaces, and a feature path rising against the previous
 * window. Nothing is recommended during a cold start. The thresholds and wording are the React app's.
 */
@Component
@RequiredArgsConstructor
public class Recommendations {
    /** Below this many events, or {@link #COLD_START_SESSIONS} sessions, no conclusions are drawn. */
    static final int COLD_START_EVENTS = 10;
    static final int COLD_START_SESSIONS = 3;

    static final int RELIABILITY_MIN_ERRORS = 3;
    static final int RELIABILITY_MIN_FAILURE_RATE = 20;
    static final int DROP_OFF_MIN_VIEWED_SESSIONS = 5;
    /** A drop-off is flagged when fewer than this share of viewing sessions open a surface. */
    static final double DROP_OFF_OPEN_SHARE = 0.7;
    static final int RISING_MIN_CURRENT = 5;
    static final int RISING_MIN_PREVIOUS = 2;
    static final double RISING_GROWTH = 1.5;

    static final int HIGH_CONFIDENCE_SAMPLE = 30;
    static final int MEDIUM_CONFIDENCE_SAMPLE = 10;
    static final String HIGH = "high";
    static final String MEDIUM = "medium";
    static final String LOW = "low";

    private static final String UNKNOWN_CODE = "unknown";

    private final ActivityInsightsProperties properties;

    public boolean isColdStart(UsageSummary summary) {
        return summary.eventCount() < COLD_START_EVENTS || summary.sessionCount() < COLD_START_SESSIONS;
    }

    public List<Recommendation> recommend(UsageSummary summary, List<UsageTrend> trends, List<FunnelStage> funnel,
            List<ErrorSummary> errors) {
        if (isColdStart(summary)) return List.of();
        var recommendations = new ArrayList<Recommendation>();
        reliabilityReview(summary, errors).ifPresent(recommendations::add);
        funnelDropOff(summary, funnel).ifPresent(recommendations::add);
        risingPath(summary, trends).ifPresent(recommendations::add);
        return recommendations.stream().limit(properties.maxRecommendations()).toList();
    }

    private static Optional<Recommendation> reliabilityReview(UsageSummary summary, List<ErrorSummary> errors) {
        var failureRate = summary.failureRate() == null ? 0 : summary.failureRate();
        if (summary.errorCount() < RELIABILITY_MIN_ERRORS || failureRate < RELIABILITY_MIN_FAILURE_RATE) {
            return Optional.empty();
        }
        var topCode = errors.isEmpty() ? UNKNOWN_CODE : errors.getFirst().code();
        return Optional.of(Recommendation.builder()
                .id("reliability-review")
                .title("Review the highest-frequency failure path")
                .summary("Failures are a meaningful share of the selected activity window.")
                .evidence("%d error events across %d events (%d%% failure outcomes). The most frequent code is %s."
                        .formatted(summary.errorCount(), summary.eventCount(), failureRate, displayKey(topCode)))
                .confidence(confidenceFor(summary.eventCount()))
                .suggestedAction("Suggested action: inspect the affected surface and reproduce the coded failure"
                        + " before changing product behavior.")
                .build());
    }

    private static Optional<Recommendation> funnelDropOff(UsageSummary summary, List<FunnelStage> funnel) {
        var views = stage(funnel, Funnel.SURFACE_VIEWS);
        var opens = stage(funnel, Funnel.SUCCESSFUL_OPENS);
        if (views.isEmpty() || opens.isEmpty()) return Optional.empty();
        var viewed = views.get().sessionCount();
        var opened = opens.get().sessionCount();
        if (viewed < DROP_OFF_MIN_VIEWED_SESSIONS || opened >= viewed * DROP_OFF_OPEN_SHARE) return Optional.empty();
        var openPercent = opens.get().conversionPercent() == null ? 0 : opens.get().conversionPercent();
        return Optional.of(Recommendation.builder()
                .id("funnel-drop-off")
                .title("Investigate the surface-open drop-off")
                .summary("Fewer sessions reached a successful surface open than viewed a surface.")
                .evidence("%d of %d viewed sessions reached a successful open (%d%% of started sessions)."
                        .formatted(opened, viewed, openPercent))
                .confidence(confidenceFor(summary.sessionCount()))
                .suggestedAction("Suggested action: compare the affected loading and error states with the intended"
                        + " entry flow; no automatic rearrangement is applied.")
                .build());
    }

    private static Optional<Recommendation> risingPath(UsageSummary summary, List<UsageTrend> trends) {
        return trends.stream()
                .filter(trend -> trend.currentCount() >= RISING_MIN_CURRENT
                        && trend.previousCount() >= RISING_MIN_PREVIOUS
                        && trend.currentCount() >= trend.previousCount() * RISING_GROWTH)
                .findFirst()
                .map(trend -> Recommendation.builder()
                        .id("rising-" + trend.key())
                        .title("Review the rising " + displayKey(trend.feature()) + " path")
                        .summary("This stable feature/action path appeared more often than in the preceding window.")
                        .evidence("%s / %s occurred %d times versus %d previously (%d%% change)."
                                .formatted(displayKey(trend.feature()), displayKey(trend.action()),
                                        trend.currentCount(), trend.previousCount(), trend.changePercent()))
                        .confidence(confidenceFor(summary.eventCount()))
                        .suggestedAction("Suggested action: decide whether this usage pattern deserves product review;"
                                + " usage alone does not trigger a change.")
                        .build());
    }

    private static Optional<FunnelStage> stage(List<FunnelStage> funnel, String id) {
        return funnel.stream().filter(stage -> stage.id().equals(id)).findFirst();
    }

    static String confidenceFor(long sampleSize) {
        if (sampleSize >= HIGH_CONFIDENCE_SAMPLE) return HIGH;
        if (sampleSize >= MEDIUM_CONFIDENCE_SAMPLE) return MEDIUM;
        return LOW;
    }

    /** Taxonomy keys read as words: {@code surface_opened} becomes {@code surface opened}. */
    static String displayKey(String value) {
        return value.replace('_', ' ');
    }
}
