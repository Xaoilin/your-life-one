package com.yourlifeone.calendar.infrastructure.crypto;

import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.config.GoogleCredentialProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Encrypts Google refresh tokens at rest with AES-256-GCM. Every write uses a fresh random nonce, and
 * the owning user and Google account are bound in as associated data, so a ciphertext copied to
 * another row fails to decrypt. Tokens never appear in exceptions or logs.
 */
@Component
public class GoogleCredentialCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    public static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String AAD_SEPARATOR = "\n";

    private final SecretKey key;
    private final int keyVersion;
    private final SecureRandom random = new SecureRandom();

    public GoogleCredentialCipher(GoogleCredentialProperties properties) {
        this.key = properties.configured() ? new SecretKeySpec(properties.keyBytes(), ALGORITHM) : null;
        this.keyVersion = properties.keyVersion();
    }

    public EncryptedToken encrypt(String token, UUID userId, String email) {
        var nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        try {
            var cipher = cipher(Cipher.ENCRYPT_MODE, nonce, userId, email);
            return new EncryptedToken(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)), nonce, keyVersion);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Google credential encryption failed", exception);
        }
    }

    /** @throws CredentialDecryptionException when the token cannot be read with this key for this owner */
    public String decrypt(EncryptedToken encrypted, UUID userId, String email) {
        if (encrypted.keyVersion() != keyVersion) {
            throw new CredentialDecryptionException("Google credential was encrypted with key version "
                    + encrypted.keyVersion() + ", not " + keyVersion, null);
        }
        try {
            var cipher = cipher(Cipher.DECRYPT_MODE, encrypted.nonce(), userId, email);
            return new String(cipher.doFinal(encrypted.ciphertext()), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new CredentialDecryptionException("Google credential could not be decrypted", exception);
        }
    }

    private Cipher cipher(int mode, byte[] nonce, UUID userId, String email) throws GeneralSecurityException {
        if (key == null) {
            throw new GoogleUnavailableException("app.calendar.google-credential.key is not configured", null);
        }
        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD((userId + AAD_SEPARATOR + email).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}
