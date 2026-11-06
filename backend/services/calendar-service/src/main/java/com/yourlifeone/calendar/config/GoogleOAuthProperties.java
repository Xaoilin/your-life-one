package com.yourlifeone.calendar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The Google OAuth client this service exchanges codes and refreshes tokens with
 * ({@code app.calendar.google-oauth.*}).
 *
 * @param clientId {@code GOOGLE_OAUTH_CLIENT_ID}
 * @param clientSecret {@code GOOGLE_OAUTH_CLIENT_SECRET}
 * @param tokenUrl Google's token endpoint
 * @param revokeUrl Google's token revocation endpoint
 */
@ConfigurationProperties("app.calendar.google-oauth")
public record GoogleOAuthProperties(String clientId, String clientSecret, String tokenUrl, String revokeUrl) {
    public boolean configured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /** Never print the client secret. */
    @Override
    public String toString() {
        return "GoogleOAuthProperties[clientId=" + clientId + ", tokenUrl=" + tokenUrl + ", revokeUrl=" + revokeUrl + "]";
    }
}
