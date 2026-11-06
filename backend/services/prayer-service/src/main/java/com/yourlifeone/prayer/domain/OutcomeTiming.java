package com.yourlifeone.prayer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Self-reported completion timing; never inferred from when an outcome was logged. */
public enum OutcomeTiming {
    PROMPT("prompt", OutcomeStatus.ON_TIME),
    WITHIN_WINDOW("within_window", OutcomeStatus.ON_TIME),
    OUTSIDE_WINDOW("outside_window", OutcomeStatus.LATE);

    private final String value;
    private final OutcomeStatus status;

    OutcomeTiming(String value, OutcomeStatus status) {
        this.value = value;
        this.status = status;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public boolean matches(OutcomeStatus outcomeStatus) {
        return status == outcomeStatus;
    }

    @JsonCreator
    public static OutcomeTiming fromValue(String value) {
        for (var timing : values()) {
            if (timing.value.equals(value)) return timing;
        }
        throw new IllegalArgumentException("Unknown outcome timing: " + value);
    }

    public static OutcomeTiming defaultFor(OutcomeStatus status) {
        return switch (status) {
            case ON_TIME -> WITHIN_WINDOW;
            case LATE -> OUTSIDE_WINDOW;
            case MISSED, UNCLASSIFIED -> null;
        };
    }
}
