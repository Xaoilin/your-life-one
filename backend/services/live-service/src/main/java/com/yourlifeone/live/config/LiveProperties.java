package com.yourlifeone.live.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The live-update streams ({@code app.live.*}).
 *
 * @param heartbeat how often a quiet stream sends a comment line, so proxies keep it open
 * @param maxStreamDuration the longest a stream stays open; it also ends when its access token expires
 * @param maxStreamsPerUser open streams per user; opening one more closes the oldest
 */
@ConfigurationProperties("app.live")
public record LiveProperties(Duration heartbeat, Duration maxStreamDuration, int maxStreamsPerUser) {
    public LiveProperties {
        if (heartbeat == null || maxStreamDuration == null || maxStreamsPerUser < 1) {
            throw new IllegalArgumentException("app.live needs heartbeat, max-stream-duration and max-streams-per-user");
        }
    }
}
