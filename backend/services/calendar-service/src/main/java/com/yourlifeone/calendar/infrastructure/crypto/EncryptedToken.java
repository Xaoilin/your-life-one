package com.yourlifeone.calendar.infrastructure.crypto;

/**
 * A token encrypted by {@link GoogleCredentialCipher}.
 *
 * @param ciphertext the AES-GCM output, tag included
 * @param nonce the random 12-byte nonce it was encrypted with
 * @param keyVersion the configured key version it was encrypted with
 */
public record EncryptedToken(byte[] ciphertext, byte[] nonce, int keyVersion) {
    /** Never print the ciphertext. */
    @Override
    public String toString() {
        return "EncryptedToken[keyVersion=" + keyVersion + "]";
    }
}
