package com.yourlifeone.calendar.application.service;

import com.yourlifeone.calendar.application.model.GoogleTokenGrant;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.infrastructure.crypto.CredentialDecryptionException;
import com.yourlifeone.calendar.infrastructure.crypto.EncryptedToken;
import com.yourlifeone.calendar.infrastructure.crypto.GoogleCredentialCipher;
import com.yourlifeone.calendar.infrastructure.persistence.entity.GoogleCredentialEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.GoogleCredentialId;
import com.yourlifeone.calendar.infrastructure.persistence.repository.GoogleCredentialRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Each user's stored Google refresh tokens, encrypted at rest. Short transactions only: Google is
 * never called while a database connection is held.
 */
@Component
@RequiredArgsConstructor
public class GoogleCredentialStore {
    /** Bounds from the google_credential table. */
    private static final int MAX_SCOPE_LENGTH = 2000;
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final GoogleCredentialRepository credentials;
    private final GoogleCredentialCipher cipher;
    private final Clock clock;
    private final CalendarSyncFreshness freshness;

    /**
     * Stores a newly granted credential. When Google issued no refresh token, the one already stored
     * for this account is kept.
     *
     * @return false when there is no refresh token to keep: nothing was stored
     */
    @Transactional
    public boolean save(UUID userId, String email, GoogleTokenGrant grant) {
        freshness.invalidate(userId);
        var existing = credentials.findById(new GoogleCredentialId(userId, email));
        if (grant.refreshToken() == null && existing.isEmpty()) return false;
        var now = clock.instant();
        var credential = existing.orElseGet(() -> GoogleCredentialEntity.builder()
                .userId(userId).email(email).createdAt(now).build());
        if (grant.refreshToken() != null) writeToken(credential, grant.refreshToken());
        if (grant.scope() != null) credential.setScope(bounded(grant.scope(), MAX_SCOPE_LENGTH));
        clearFailure(credential);
        credential.setUpdatedAt(now);
        credentials.save(credential);
        return true;
    }

    /**
     * The decrypted refresh token, or empty when none is stored.
     *
     * @throws CredentialDecryptionException when the stored token cannot be read with the current key
     */
    @Transactional(readOnly = true)
    public Optional<String> refreshToken(UUID userId, String email) {
        return credentials.findById(new GoogleCredentialId(userId, email))
                .map(credential -> cipher.decrypt(new EncryptedToken(credential.getRefreshTokenCiphertext(),
                        credential.getRefreshTokenNonce(), credential.getKeyVersion()), userId, email));
    }

    /** Google accepted the refresh token; a rotated refresh token replaces the stored one. */
    @Transactional
    public void recordRefresh(UUID userId, String email, GoogleTokenGrant grant) {
        credentials.findById(new GoogleCredentialId(userId, email)).ifPresent(credential -> {
            var now = clock.instant();
            if (grant.refreshToken() != null) writeToken(credential, grant.refreshToken());
            if (grant.scope() != null) credential.setScope(bounded(grant.scope(), MAX_SCOPE_LENGTH));
            credential.setLastRefreshAt(now);
            clearFailure(credential);
            credential.setUpdatedAt(now);
        });
    }

    /** Google refused the credential; the user must connect the account again. */
    @Transactional
    public void recordFailure(UUID userId, String email, AuthStatus status, String reason) {
        freshness.invalidate(userId);
        credentials.findById(new GoogleCredentialId(userId, email)).ifPresent(credential -> {
            var now = clock.instant();
            credential.setLastFailureReason(bounded(reason, MAX_FAILURE_REASON_LENGTH));
            credential.setLastFailureAt(now);
            credential.setRevokedAt(status == AuthStatus.REVOKED ? now : null);
            credential.setUpdatedAt(now);
        });
    }

    /** Succeeds when nothing is stored. */
    @Transactional
    public void delete(UUID userId, String email) {
        freshness.invalidate(userId);
        credentials.deleteById(new GoogleCredentialId(userId, email));
    }

    private void writeToken(GoogleCredentialEntity credential, String refreshToken) {
        var encrypted = cipher.encrypt(refreshToken, credential.getUserId(), credential.getEmail());
        credential.setRefreshTokenCiphertext(encrypted.ciphertext());
        credential.setRefreshTokenNonce(encrypted.nonce());
        credential.setKeyVersion((short) encrypted.keyVersion());
    }

    private static void clearFailure(GoogleCredentialEntity credential) {
        credential.setLastFailureReason(null);
        credential.setLastFailureAt(null);
        credential.setRevokedAt(null);
    }

    private static String bounded(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
