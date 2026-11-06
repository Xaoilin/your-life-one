package com.yourlifeone.planner.clock.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.clock.config.ClockProperties;
import com.yourlifeone.planner.clock.domain.CountdownTimer;
import com.yourlifeone.planner.clock.domain.Stopwatch;
import com.yourlifeone.planner.shared.AllowedValues;
import com.yourlifeone.planner.shared.RecordIds;
import com.yourlifeone.planner.shared.Texts;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Checks a stopwatch or timer the app saves. */
@Component
@RequiredArgsConstructor
public class ClockRules {
    private static final String RUNNING = "running";
    private static final int MAX_LABEL = 100;

    private final ClockProperties properties;

    /** @param label used when the app sends none */
    public Stopwatch stopwatch(Stopwatch requested, String label) {
        RecordIds.require(requested.id());
        var laps = requested.laps() == null ? List.<Long>of() : List.copyOf(requested.laps());
        if (laps.size() > properties.maxLaps()) {
            throw ApiException.badRequest("too_many_laps", "A stopwatch keeps at most " + properties.maxLaps() + " laps.");
        }
        return requested.toBuilder().label(label(requested.label(), label)).laps(laps).build();
    }

    /** @param label used when the app sends none */
    public CountdownTimer timer(CountdownTimer requested, String label) {
        RecordIds.require(requested.id());
        var status = AllowedValues.require("status", requested.status(), properties.timerStatuses());
        if (requested.durationMs() < 1 || requested.durationMs() > properties.maxDurationMs()) {
            throw ApiException.badRequest("invalid_duration",
                    "A timer lasts between 1 ms and " + properties.maxDurationMs() + " ms.");
        }
        if (requested.remainingMs() < 0 || requested.remainingMs() > requested.durationMs()) {
            throw ApiException.badRequest("invalid_remaining", "remainingMs must be between 0 and durationMs.");
        }
        if (RUNNING.equals(status) != (requested.endsAt() != null)) {
            throw ApiException.badRequest("invalid_end", "Only a running timer has endsAt, and it must have one.");
        }
        return requested.toBuilder()
                .label(label(requested.label(), label))
                .status(status)
                .sound(AllowedValues.require("sound", requested.sound(), properties.timerSounds()))
                .build();
    }

    private static String label(String requested, String fallback) {
        var label = Texts.orDefault(requested, fallback);
        return label.length() > MAX_LABEL ? label.substring(0, MAX_LABEL) : label;
    }
}
