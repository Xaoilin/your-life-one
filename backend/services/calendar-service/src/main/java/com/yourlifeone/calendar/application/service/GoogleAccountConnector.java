package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.commonservice.web.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Connecting and disconnecting Google accounts. The browser only obtains a one-time authorization
 * code from Google's consent popup; the credential it grants never reaches the browser, and the
 * Your Life One session is never replaced.
 */
@Service
@RequiredArgsConstructor
public class GoogleAccountConnector {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAccountConnector.class);

    private final GoogleCredentials credentials;
    private final GoogleAccessTokens googleTokens;
    private final AccountService accounts;
    private final CalendarSyncService sync;

    /**
     * Stores the account's credential, adds or reconnects the account and mirrors its calendars. A
     * failed first sync is recorded on the account rather than failing the connection.
     */
    public CalendarAccount connect(UUID userId, String code, String redirectUri, String expectedEmail) {
        var connected = credentials.connect(userId, code, redirectUri, expectedEmail);
        // A reconnected account starts from its new credential, not a token cached from the old one.
        googleTokens.discard(userId, connected.email());
        var account = accounts.registerGoogle(userId, connected.email(), connected.name());
        sync.sync(userId, account.id());
        return accounts.get(userId, account.id());
    }

    /** Revokes a removed account's Google credential; a failure is logged, the account is already gone. */
    public void revokeQuietly(UUID userId, CalendarAccount account) {
        googleTokens.discard(userId, account.email());
        try {
            credentials.revoke(userId, account.email());
        } catch (ApiException exception) {
            logger.warn("Google credential was not revoked: code={} message={}", exception.code(), exception.getMessage());
        }
    }
}
