package com.yourlifeone.prayer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** The Daily Momentum pillars that can prompt after a prayer, in the order their notices list them. */
public enum Pillar {
    LEARN("learn"),
    MOVE("move");

    private final String label;

    Pillar(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static Pillar fromLabel(String label) {
        for (var pillar : values()) {
            if (pillar.label.equals(label)) return pillar;
        }
        throw new IllegalArgumentException("Unknown pillar: " + label);
    }
}
