package com.yourlifeone.prayer.domain;

/** Every daily timing the schedule publishes, in display order. */
public enum Timing {
    FAJR("Fajr", "الفجر", true),
    SUNRISE("Sunrise", "الشروق", false),
    DHUHR("Dhuhr", "الظهر", true),
    ASR("Asr", "العصر", true),
    SUNSET("Sunset", "غروب", false),
    MAGHRIB("Maghrib", "المغرب", true),
    ISHA("Isha", "العشاء", true),
    MIDNIGHT("Midnight", "نصف الليل", false);

    private final String label;
    private final String arabic;
    private final boolean prayer;

    Timing(String label, String arabic, boolean prayer) {
        this.label = label;
        this.arabic = arabic;
        this.prayer = prayer;
    }

    public String label() {
        return label;
    }

    public String arabic() {
        return arabic;
    }

    /** True for an obligatory prayer; false for a reference event such as sunrise. */
    public boolean isPrayer() {
        return prayer;
    }
}
