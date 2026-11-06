package com.yourlifeone.planner.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The services the planner reads from over HTTP ({@code app.planner.upstream.*}), forwarding the user's
 * token, instead of reading their tables.
 *
 * @param prayerUrl base URL of the prayer service, e.g. {@code https://192.0.2.10}
 * @param knowledgeUrl base URL of the knowledge service
 * @param connectTimeout how long to wait for a connection
 * @param readTimeout how long to wait for an answer; the app gives up on a write after 10 seconds
 */
@ConfigurationProperties("app.planner.upstream")
public record UpstreamProperties(String prayerUrl, String knowledgeUrl, Duration connectTimeout, Duration readTimeout) {
    public UpstreamProperties {
        requireUrl("prayer-url", prayerUrl);
        requireUrl("knowledge-url", knowledgeUrl);
        if (connectTimeout == null || readTimeout == null) {
            throw new IllegalArgumentException("app.planner.upstream timeouts are required");
        }
    }

    private static void requireUrl(String name, String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("app.planner.upstream." + name + " is required");
        }
    }
}
