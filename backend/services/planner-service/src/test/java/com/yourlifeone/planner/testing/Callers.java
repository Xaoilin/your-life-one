package com.yourlifeone.planner.testing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;

/** The user an upstream request was made for: the tests' access token is the user's ID. */
final class Callers {
    private static final String BEARER = "Bearer ";

    private Callers() {
    }

    static Optional<UUID> of(ClientHttpRequest request) {
        var authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER)) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(authorization.substring(BEARER.length())));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
