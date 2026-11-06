package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.model.ConnectedGoogleAccount;
import com.yourlifeone.calendar.application.model.GoogleAccessToken;
import com.yourlifeone.calendar.application.port.GoogleOAuthApi;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.infrastructure.crypto.CredentialDecryptionException;
import com.yourlifeone.calendar.infrastructure.persistence.importing.SupabaseGoogleCredentialImport;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Each user's Google credentials: this service holds the OAuth client secret and the users' refresh
 * tokens (encrypted), and hands out short-lived access tokens. Keyed by the Your Life One user and the
 * Google account's address.
 */
@Service
@RequiredArgsConstructor
public class GoogleCredentials {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCredentials.class);
    private static final String MISSING_REFRESH_TOKEN = "Google did not return a refresh token. Reconnect once more "
            + "with consent so Your Life One can store a durable Calendar credential.";
    private static final String NO_CREDENTIAL = "No Google Calendar credential is stored for this account. "
            + "Reconnect it.";
    private static final String UNREADABLE_CREDENTIAL = "The stored Google Calendar credential can no longer be "
            + "read. Reconnect this account.";

    private final GoogleOAuthApi oauth;
    private final GoogleCredentialStore store;
    private final SupabaseGoogleCredentialImport supabaseImport;

    /**
     * Exchanges an authorization code from Google's consent popup and stores the credential.
     *
     * @param expectedEmail when reconnecting, the account the user meant; another account is refused
     *     and the credential Google just granted is revoked
     */
    public ConnectedGoogleAccount connect(UUID userId, String code, String redirectUri, String expectedEmail) {
        supabaseImport.ensureImported();
        var grant = oauth.exchangeCode(code, redirectUri);
        var account = oauth.account(grant.accessToken());
        if (expectedEmail != null && !expectedEmail.isBlank() && !normalize(expectedEmail).equals(account.email())) {
            oauth.revoke(grant.accessToken());
            throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, "Google returned "
                    + account.email() + " instead of " + normalize(expectedEmail)
                    + ". Reconnect this account explicitly.");
        }
        if (!store.save(userId, account.email(), grant)) {
            throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, MISSING_REFRESH_TOKEN);
        }
        return account;
    }

    /**
     * A new access token from the stored refresh token. When Google refuses it, the failure is recorded
     * on the credential.
     *
     * @throws GoogleReconnectRequiredException when there is no usable credential
     */
    public GoogleAccessToken accessToken(UUID userId, String email) {
        supabaseImport.ensureImported();
        var account = normalize(email);
        var refreshToken = storedToken(userId, account)
                .orElseThrow(() -> new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, NO_CREDENTIAL));
        try {
            var grant = oauth.refresh(refreshToken);
            store.recordRefresh(userId, account, grant);
            return new GoogleAccessToken(grant.accessToken(), grant.expiresAt());
        } catch (GoogleReconnectRequiredException exception) {
            store.recordFailure(userId, account, exception.authStatus(), exception.getMessage());
            throw exception;
        }
    }

    /** Revokes the credential at Google and forgets it; succeeds when there is none. */
    public void revoke(UUID userId, String email) {
        supabaseImport.ensureImported();
        var account = normalize(email);
        try {
            store.refreshToken(userId, account).ifPresent(oauth::revoke);
        } catch (CredentialDecryptionException exception) {
            logger.warn("Unreadable Google credential deleted without revoking: {}", exception.getMessage());
        }
        store.delete(userId, account);
    }

    private Optional<String> storedToken(UUID userId, String email) {
        try {
            return store.refreshToken(userId, email);
        } catch (CredentialDecryptionException exception) {
            logger.warn("Stored Google credential is unreadable: {}", exception.getMessage());
            store.recordFailure(userId, email, AuthStatus.NEEDS_RECONNECT, UNREADABLE_CREDENTIAL);
            throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, UNREADABLE_CREDENTIAL);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
