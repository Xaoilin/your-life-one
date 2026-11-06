package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.config.ActivityInsightsProperties;
import com.yourlifeone.profile.activity.domain.PathCount;
import com.yourlifeone.profile.activity.domain.UsageTrend;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The most-used feature/action paths in the current window, each compared with the equal window
 * before it. Most used first; ties by path key.
 */
@Component
@RequiredArgsConstructor
public class Trends {
    private static final Comparator<UsageTrend> MOST_USED_FIRST = Comparator
            .comparingLong(UsageTrend::currentCount).reversed()
            .thenComparing(UsageTrend::key);

    private final ActivityInsightsProperties properties;

    public List<UsageTrend> rank(List<PathCount> paths) {
        return paths.stream()
                .filter(path -> path.currentCount() > 0)
                .map(Trends::toTrend)
                .sorted(MOST_USED_FIRST)
                .limit(properties.maxTrends())
                .toList();
    }

    private static UsageTrend toTrend(PathCount path) {
        return UsageTrend.builder()
                .key(path.feature() + ":" + path.action())
                .feature(path.feature())
                .action(path.action())
                .currentCount(path.currentCount())
                .previousCount(path.previousCount())
                .changePercent(Percentages.change(path.currentCount(), path.previousCount()))
                .build();
    }
}
