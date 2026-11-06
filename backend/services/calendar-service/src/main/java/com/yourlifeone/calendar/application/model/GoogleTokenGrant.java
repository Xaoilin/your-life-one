package com.yourlifeone.calendar.application.model;

import java.time.Instant;
import lombok.Builder;

/**
 * Tokens Google granted from an authorization code or a refresh token.
 *
 * @param expiresAt when the access token stops working; null when Google did not say
 * @param refreshToken null when Google did not issue a new one
 * @param scope the granted scopes, space separated; may be null
 */
@Builder
public record GoogleTokenGrant(String accessToken, Instant expiresAt, String refreshToken, String scope) {
    /** Never print the tokens. */
    @Override
    public String toString() {
        return "GoogleTokenGrant[expiresAt=" + expiresAt + ", scope=" + scope + "]";
    }
}
