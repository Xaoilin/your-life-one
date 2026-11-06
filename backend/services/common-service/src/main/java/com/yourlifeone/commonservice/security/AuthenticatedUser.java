package com.yourlifeone.commonservice.security;

import com.yourlifeone.commonservice.web.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves the Your Life One account that owns a request from its verified Supabase access token. */
public final class AuthenticatedUser {
    private AuthenticatedUser() {
    }

    public static UUID id(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_token", "The access token has no valid subject.");
        }
    }
}
