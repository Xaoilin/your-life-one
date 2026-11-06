package com.yourlifeone.life.shared;

/** Small null-handling helpers for optional fields. */
public final class Values {
    private Values() {
    }

    /** The value, or the fallback when the value is null; the fallback may itself be null. */
    public static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
