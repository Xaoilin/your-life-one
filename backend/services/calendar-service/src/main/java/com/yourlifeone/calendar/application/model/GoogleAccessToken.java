package com.yourlifeone.calendar.application.model;

import java.time.Instant;

/** @param expiresAt when Google stops accepting the token; null when Google did not say */
public record GoogleAccessToken(String value, Instant expiresAt) {
    /** Never print the token. */
    @Override
    public String toString() {
        return "GoogleAccessToken[expiresAt=" + expiresAt + "]";
    }
}
