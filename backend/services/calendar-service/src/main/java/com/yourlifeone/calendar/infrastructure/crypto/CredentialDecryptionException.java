package com.yourlifeone.calendar.infrastructure.crypto;

/**
 * A stored token could not be decrypted: it was changed, belongs to another user or account, or was
 * encrypted with a key this service no longer has. Carries no token material.
 */
public class CredentialDecryptionException extends RuntimeException {
    public CredentialDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
