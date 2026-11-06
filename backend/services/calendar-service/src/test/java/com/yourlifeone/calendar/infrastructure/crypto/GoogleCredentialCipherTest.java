package com.yourlifeone.calendar.infrastructure.crypto;

import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.config.GoogleCredentialProperties;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleCredentialCipherTest {
    private static final String TOKEN = "1//0refresh-token-value";
    private static final String EMAIL = "yourlife@example.com";
    private static final UUID USER = UUID.randomUUID();

    private static GoogleCredentialProperties key(int fill, int version) {
        var bytes = new byte[GoogleCredentialProperties.KEY_BYTES];
        Arrays.fill(bytes, (byte) fill);
        return new GoogleCredentialProperties(Base64.getEncoder().encodeToString(bytes), version);
    }

    private final GoogleCredentialCipher cipher = new GoogleCredentialCipher(key(7, 1));

    @Test
    void decryptsWhatItEncryptedWithAFreshNonceEachTime() {
        var first = cipher.encrypt(TOKEN, USER, EMAIL);
        var second = cipher.encrypt(TOKEN, USER, EMAIL);

        assertEquals(TOKEN, cipher.decrypt(first, USER, EMAIL));
        assertEquals(GoogleCredentialCipher.NONCE_BYTES, first.nonce().length);
        assertFalse(Arrays.equals(first.nonce(), second.nonce()));
        assertFalse(new String(first.ciphertext(), StandardCharsets.ISO_8859_1).contains(TOKEN));
    }

    @Test
    void refusesATamperedCiphertext() {
        var encrypted = cipher.encrypt(TOKEN, USER, EMAIL);
        var tampered = encrypted.ciphertext().clone();
        tampered[0] ^= 1;

        assertThrows(CredentialDecryptionException.class,
                () -> cipher.decrypt(new EncryptedToken(tampered, encrypted.nonce(), 1), USER, EMAIL));
    }

    @Test
    void refusesACiphertextMovedToAnotherUserOrAccount() {
        var encrypted = cipher.encrypt(TOKEN, USER, EMAIL);

        assertThrows(CredentialDecryptionException.class, () -> cipher.decrypt(encrypted, UUID.randomUUID(), EMAIL));
        assertThrows(CredentialDecryptionException.class, () -> cipher.decrypt(encrypted, USER, "other@example.com"));
    }

    @Test
    void refusesATokenEncryptedWithAnotherKey() {
        var encrypted = cipher.encrypt(TOKEN, USER, EMAIL);

        assertThrows(CredentialDecryptionException.class,
                () -> new GoogleCredentialCipher(key(9, 1)).decrypt(encrypted, USER, EMAIL));
        assertThrows(CredentialDecryptionException.class,
                () -> new GoogleCredentialCipher(key(7, 2)).decrypt(encrypted, USER, EMAIL));
    }

    @Test
    void keyIsValidatedAndNeverPrinted() {
        var properties = key(7, 1);

        assertFalse(properties.toString().contains(properties.key()));
        assertThrows(IllegalArgumentException.class, () -> new GoogleCredentialProperties("c2hvcnQ=", 1));
        assertThrows(GoogleUnavailableException.class,
                () -> new GoogleCredentialCipher(new GoogleCredentialProperties("", 1)).encrypt(TOKEN, USER, EMAIL));
    }
}
