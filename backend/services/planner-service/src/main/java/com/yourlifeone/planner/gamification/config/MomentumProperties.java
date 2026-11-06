package com.yourlifeone.planner.gamification.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Daily momentum ({@code app.planner.momentum.*}).
 *
 * @param pillars the pillars the app keeps a state for, e.g. {@code learn} and {@code move}
 * @param maxStateChars the largest state one pillar may store, in characters of JSON
 */
@ConfigurationProperties("app.planner.momentum")
public record MomentumProperties(List<String> pillars, int maxStateChars) {
    public MomentumProperties {
        pillars = pillars == null ? List.of() : List.copyOf(pillars);
    }
}
