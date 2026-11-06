package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.PathCount;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrendsTest {
    private final Trends trends = new Trends(InsightTestProperties.DEFAULTS);

    private static PathCount path(String feature, String action, long current, long previous) {
        return PathCount.builder().feature(feature).action(action).currentCount(current).previousCount(previous).build();
    }

    @Test
    void comparesEachPathWithThePreviousWindowRoundingHalfUp() {
        var ranked = trends.rank(List.of(
                path("navigation", "surface_viewed", 8, 4),
                path("calendar", "opened", 2, 3),
                path("tasks", "created", 1, 0)));

        assertEquals("navigation:surface_viewed", ranked.get(0).key());
        assertEquals(100, ranked.get(0).changePercent());
        // (2 - 3) / 3 = -33.3%
        assertEquals(-33, ranked.get(1).changePercent());
        assertNull(ranked.get(2).changePercent(), "a path absent before is new");
    }

    @Test
    void listsOnlyPathsUsedNowMostUsedFirstThenByKeyAndKeepsTheTopEight() {
        var paths = new ArrayList<>(IntStream.range(0, 10)
                .mapToObj(index -> path("feature_" + index, "used", index + 1, 0)).toList());
        paths.add(path("gone", "used", 0, 5));
        paths.add(path("alpha", "used", 10, 0));

        var ranked = trends.rank(paths);

        assertEquals(InsightTestProperties.MAX_TRENDS, ranked.size());
        assertEquals(List.of("alpha:used", "feature_9:used"), ranked.subList(0, 2).stream().map(t -> t.key()).toList());
        assertEquals(0, ranked.stream().filter(trend -> trend.feature().equals("gone")).count());
    }
}
