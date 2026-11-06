package com.yourlifeone.prayer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OutcomeStatus {
    ON_TIME("on_time"),
    LATE("late"),
    MISSED("missed"),
    /** Imported from before outcomes were classified; never written through the API. */
    UNCLASSIFIED("unclassified");

    private final String value;

    OutcomeStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    /** Prayed (on time or late); earns the one-time reward. */
    public boolean isCompleted() {
        return this == ON_TIME || this == LATE;
    }

    @JsonCreator
    public static OutcomeStatus fromValue(String value) {
        var status = parseOrNull(value);
        if (status == null) throw new IllegalArgumentException("Unknown outcome status: " + value);
        return status;
    }

    public static OutcomeStatus parseOrNull(Object value) {
        if (!(value instanceof String text)) return null;
        for (var status : values()) {
            if (status.value.equals(text)) return status;
        }
        return null;
    }
}
