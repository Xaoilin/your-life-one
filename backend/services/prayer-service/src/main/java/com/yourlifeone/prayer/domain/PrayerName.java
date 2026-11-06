package com.yourlifeone.prayer.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** The five daily prayers, each on time until the start of its deadline timing. */
public enum PrayerName {
    FAJR("Fajr", Timing.SUNRISE),
    DHUHR("Dhuhr", Timing.ASR),
    ASR("Asr", Timing.MAGHRIB),
    MAGHRIB("Maghrib", Timing.ISHA),
    ISHA("Isha", Timing.MIDNIGHT);

    private final String label;
    private final Timing deadline;

    PrayerName(String label, Timing deadline) {
        this.label = label;
        this.deadline = deadline;
    }

    @JsonValue
    public String label() {
        return label;
    }

    public Timing timing() {
        return Timing.valueOf(name());
    }

    public Timing deadline() {
        return deadline;
    }

    @JsonCreator
    public static PrayerName fromLabel(String label) {
        for (var prayer : values()) {
            if (prayer.label.equals(label)) return prayer;
        }
        throw new IllegalArgumentException("Unknown prayer: " + label);
    }

    public static PrayerName parseOrNull(Object label) {
        if (!(label instanceof String text)) return null;
        for (var prayer : values()) {
            if (prayer.label.equals(text)) return prayer;
        }
        return null;
    }
}
