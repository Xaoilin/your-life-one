package com.yourlifeone.planner.clock.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The clock workspace ({@code app.planner.clock.*}).
 *
 * @param timerStatuses a timer's states; only {@code running} has an end
 * @param maxLaps the most laps one stopwatch keeps
 * @param maxDurationMs the longest timer
 */
@ConfigurationProperties("app.planner.clock")
public record ClockProperties(List<String> timerSounds, List<String> timerStatuses, int maxLaps, long maxDurationMs) {
    public ClockProperties {
        timerSounds = timerSounds == null ? List.of() : List.copyOf(timerSounds);
        timerStatuses = timerStatuses == null ? List.of() : List.copyOf(timerStatuses);
    }
}
