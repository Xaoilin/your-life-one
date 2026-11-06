package com.yourlifeone.prayer.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** What a prayer looks like on a given day, recorded or inferred from its window. */
public enum DayStatus {
    ON_TIME("on_time"),
    LATE("late"),
    MISSED("missed"),
    UNCLASSIFIED("unclassified"),
    /** The on-time window has not closed yet, or no timetable is known. */
    PENDING("pending"),
    /** Before tracking started. */
    NOT_TRACKED("not_tracked");

    private final String value;

    DayStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static DayStatus of(OutcomeStatus status) {
        return valueOf(status.name());
    }
}
