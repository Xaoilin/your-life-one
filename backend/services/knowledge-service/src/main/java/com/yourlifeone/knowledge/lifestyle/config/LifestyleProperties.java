package com.yourlifeone.knowledge.lifestyle.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lifestyle tracker columns and statuses ({@code app.knowledge.lifestyle.*}). Columns belong to one of
 * two families, things to avoid and things to practise; each family has its own statuses, and the first
 * is where an item starts.
 *
 * @param avoidTypes columns of things to avoid, e.g. {@code haram}
 * @param practiceTypes columns of things to practise, e.g. {@code halal}
 * @param avoidStatuses progress while avoiding, first is the starting status
 * @param practiceStatuses progress while practising, first is the starting status
 * @param maxSources the most references one item may keep
 */
@ConfigurationProperties("app.knowledge.lifestyle")
public record LifestyleProperties(
        List<String> avoidTypes,
        List<String> practiceTypes,
        List<String> avoidStatuses,
        List<String> practiceStatuses,
        int maxSources) {
    public LifestyleProperties {
        avoidTypes = copy(avoidTypes);
        practiceTypes = copy(practiceTypes);
        avoidStatuses = copy(avoidStatuses);
        practiceStatuses = copy(practiceStatuses);
        if (avoidStatuses.isEmpty() || practiceStatuses.isEmpty()) {
            throw new IllegalArgumentException("app.knowledge.lifestyle needs statuses for both families");
        }
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
