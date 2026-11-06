package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Google's token endpoint reply: the tokens on success, {@code error} and {@code error_description}
 * on failure (https://developers.google.com/identity/protocols/oauth2/web-server).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") String scope,
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription) {
    /** Never print the tokens. */
    @Override
    public String toString() {
        return "GoogleTokenResponse[expiresIn=" + expiresIn + ", error=" + error + "]";
    }
}
