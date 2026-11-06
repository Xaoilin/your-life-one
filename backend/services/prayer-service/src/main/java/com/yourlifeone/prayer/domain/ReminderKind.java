package com.yourlifeone.prayer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * What a reminder is about: a prayer's on-time window closing soon, a prayer's window opening, or a Daily
 * Momentum pillar to do after a prayer. Only deadline reminders are sent as {@code prayer.reminder}.
 */
public enum ReminderKind {
    DEADLINE("deadline"),
    OPPORTUNITY("prayer-opportunity"),
    MOMENTUM("momentum");

    private final String label;

    ReminderKind(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static ReminderKind fromLabel(String label) {
        for (var kind : values()) {
            if (kind.label.equals(label)) return kind;
        }
        throw new IllegalArgumentException("Unknown reminder kind: " + label);
    }
}
