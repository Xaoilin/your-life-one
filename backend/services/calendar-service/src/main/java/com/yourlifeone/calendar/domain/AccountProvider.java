package com.yourlifeone.calendar.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** Where an account's calendars come from: mirrored from Google, or kept only in Your Life One. */
public enum AccountProvider {
    GOOGLE("google"),
    LOCAL("local");

    private final String value;

    AccountProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static AccountProvider fromValue(String value) {
        for (var provider : values()) {
            if (provider.value.equals(value)) return provider;
        }
        throw new IllegalArgumentException("Unknown account provider: " + value);
    }
}
