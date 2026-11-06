package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.application.model.GoogleAccessToken;
import com.yourlifeone.calendar.domain.CalendarAccount;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Short-lived Google access tokens, refreshed from the stored Google credential and kept in memory
 * (never in the database) until shortly before they expire. The API prepares the account's token
 * before queueing a Google write, so a credential problem is reported to the caller straight away.
 */
@Component
@RequiredArgsConstructor
public class GoogleAccessTokens {
    /** Google access tokens last an hour; one is reused while it has at least this long left. */
    private static final Duration MINIMUM_REMAINING = Duration.ofMinutes(5);
    /** Assumed lifetime when Google does not say. */
    private static final Duration DEFAULT_LIFETIME = Duration.ofMinutes(30);

    private final GoogleCredentials credentials;
    private final AccountService accounts;
    private final Clock clock;
    private final CalendarSyncFreshness freshness;
    private final Map<String, GoogleAccessToken> tokens = new ConcurrentHashMap<>();

    /**
     * A usable access token for a Google account. When Google refuses the stored credential, the
     * account is marked for reconnection and {@link GoogleReconnectRequiredException} is thrown.
     */
    public String ensure(UUID userId, CalendarAccount account) {
        var key = key(userId, account.email());
        var cached = tokens.get(key);
        if (cached != null && isFresh(cached)) return cached.value();
        try {
            var fetched = credentials.accessToken(userId, account.email());
            var expiresAt = fetched.expiresAt() == null ? clock.instant().plus(DEFAULT_LIFETIME) : fetched.expiresAt();
            tokens.put(key, new GoogleAccessToken(fetched.value(), expiresAt));
            return fetched.value();
        } catch (GoogleReconnectRequiredException exception) {
            tokens.remove(key);
            accounts.recordAuthFailure(userId, account.id(), exception.authStatus(), exception.getMessage());
            throw exception;
        }
    }

    /** The token {@link #ensure} prepared for this account. */
    public String current(UUID userId, String email) {
        var cached = tokens.get(key(userId, email));
        if (cached == null || cached.expiresAt().isBefore(clock.instant())) {
            throw new GoogleUnavailableException("No prepared Google access token for the account", null);
        }
        return cached.value();
    }

    /** Forgets a token Google has refused. */
    public void discard(UUID userId, String email) {
        freshness.invalidate(userId);
        tokens.remove(key(userId, email));
    }

    private boolean isFresh(GoogleAccessToken token) {
        return token.expiresAt().isAfter(clock.instant().plus(MINIMUM_REMAINING));
    }

    private static String key(UUID userId, String email) {
        return userId + "\n" + email;
    }
}
