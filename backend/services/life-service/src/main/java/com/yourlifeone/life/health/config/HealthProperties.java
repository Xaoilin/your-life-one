package com.yourlifeone.life.health.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed values in the health journal ({@code app.life.health.*}).
 *
 * @param ratings how a fast-food meal went, e.g. {@code good}
 * @param symptoms how the user felt afterwards, e.g. {@code bloated}
 */
@ConfigurationProperties("app.life.health")
public record HealthProperties(List<String> ratings, List<String> symptoms) {
    public HealthProperties {
        ratings = ratings == null ? List.of() : List.copyOf(ratings);
        symptoms = symptoms == null ? List.of() : List.copyOf(symptoms);
    }
}
