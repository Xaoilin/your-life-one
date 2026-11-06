package com.yourlifeone.planner.shared;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Small text helpers for normalising what the app sends. */
public final class Texts {
    private Texts() {
    }

    /** Trimmed text; empty when null. */
    public static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    /** Trimmed text, or the fallback when that is empty. */
    public static String orDefault(String value, String fallback) {
        var trimmed = trimmed(value);
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    /** Trimmed text, or null when empty. */
    public static String blankToNull(String value) {
        var trimmed = trimmed(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Trimmed, non-empty values without repeats, in first-seen order. */
    public static List<String> distinctTrimmed(Collection<String> values) {
        if (values == null) return List.of();
        var distinct = new LinkedHashSet<String>();
        values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty())
                .forEach(distinct::add);
        return List.copyOf(distinct);
    }
}
