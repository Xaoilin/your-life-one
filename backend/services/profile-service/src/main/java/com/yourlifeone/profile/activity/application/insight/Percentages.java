package com.yourlifeone.profile.activity.application.insight;

/** Whole-number percentages, rounded half up as the React app did. */
final class Percentages {
    private static final double HUNDRED = 100.0;

    private Percentages() {
    }

    /** {@code value} as a share of {@code total}; null when there is no total. */
    static Integer of(long value, long total) {
        if (total <= 0) return null;
        return (int) Math.round(((double) value / total) * HUNDRED);
    }

    /** The change from {@code previous} to {@code current}; null when there was nothing before. */
    static Integer change(long current, long previous) {
        if (previous == 0) return null;
        return (int) Math.round(((double) (current - previous) / previous) * HUNDRED);
    }
}
