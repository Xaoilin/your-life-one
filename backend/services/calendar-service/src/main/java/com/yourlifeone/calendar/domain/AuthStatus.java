package com.yourlifeone.calendar.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/** Whether Your Life One can reach an account's Google Calendar. Local accounts are always connected. */
public enum AuthStatus {
    CONNECTED("connected"),
    /** Google refused the stored credential; the user must connect the account again. */
    NEEDS_RECONNECT("needs_reconnect"),
    /** The user or Google revoked access. */
    REVOKED("revoked"),
    ERROR("error");

    private final String value;

    AuthStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static AuthStatus fromValue(String value) {
        for (var status : values()) {
            if (status.value.equals(value)) return status;
        }
        throw new IllegalArgumentException("Unknown auth status: " + value);
    }
}
