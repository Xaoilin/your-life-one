package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.profile.activity.application.insight.Errors;
import com.yourlifeone.profile.activity.application.insight.Funnel;
import com.yourlifeone.profile.activity.application.insight.Recommendations;
import com.yourlifeone.profile.activity.application.insight.Summaries;
import com.yourlifeone.profile.activity.application.insight.Trends;
import com.yourlifeone.profile.activity.domain.ActivityFilters;
import com.yourlifeone.profile.activity.domain.ActivityInsights;
import com.yourlifeone.profile.activity.infrastructure.persistence.ActivityInsightsRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Activity page's insights for one set of filters: the current window ends now, and trends compare
 * it with the equal window before it.
 */
@Service
@RequiredArgsConstructor
public class ActivityInsightsService {
    private final ActivityInsightsRepository repository;
    private final Summaries summaries;
    private final Trends trends;
    private final Funnel funnel;
    private final Errors errors;
    private final Recommendations recommendations;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ActivityInsights insights(UUID userId, ActivityFilters filters) {
        var end = clock.instant();
        var window = Duration.ofDays(filters.rangeDays());
        var currentStart = end.minus(window);
        var previousStart = currentStart.minus(window);

        var summary = summaries.summarise(repository.summary(userId, filters, currentStart, end));
        var topTrends = trends.rank(repository.paths(userId, filters, previousStart, currentStart, end));
        var stages = funnel.stages(repository.sessionProgress(userId, filters, currentStart, end));
        var topErrors = errors.rank(repository.errors(userId, filters, currentStart, end, Errors.APPLICATION));
        return ActivityInsights.builder()
                .rangeDays(filters.rangeDays())
                .totalEventCount(repository.countAll(userId))
                .features(repository.features(userId).stream().sorted().toList())
                .summary(summary)
                .trends(topTrends)
                .funnel(stages)
                .errors(topErrors)
                .recommendations(recommendations.recommend(summary, topTrends, stages, topErrors))
                .coldStart(recommendations.isColdStart(summary))
                .build();
    }
}
