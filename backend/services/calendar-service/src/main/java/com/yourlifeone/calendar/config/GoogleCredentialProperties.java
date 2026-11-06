package com.yourlifeone.calendar.config;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How stored Google refresh tokens are encrypted ({@code app.calendar.google-credential.*}).
 *
 * @param key base64 of a 32-byte AES key ({@code CALENDAR_GOOGLE_CREDENTIAL_KEY}); blank leaves Google
 *     accounts unusable until one is configured. Losing it means every user reconnects Google.
 * @param keyVersion stored with each token, so a token encrypted with another key is recognised
 */
@ConfigurationProperties("app.calendar.google-credential")
public record GoogleCredentialProperties(String key, int keyVersion) {
    /** AES-256. */
    public static final int KEY_BYTES = 32;

    public GoogleCredentialProperties {
        if (keyVersion < 1) {
            throw new IllegalArgumentException("app.calendar.google-credential.key-version must be positive");
        }
        if (key != null && !key.isBlank() && decode(key).length != KEY_BYTES) {
            throw new IllegalArgumentException("app.calendar.google-credential.key must be base64 of "
                    + KEY_BYTES + " bytes");
        }
    }

    public boolean configured() {
        return key != null && !key.isBlank();
    }

    /** The raw key; only call when {@link #configured()}. */
    public byte[] keyBytes() {
        return decode(key);
    }

    /** Never print the key. */
    @Override
    public String toString() {
        return "GoogleCredentialProperties[keyVersion=" + keyVersion + ", configured=" + configured() + "]";
    }

    private static byte[] decode(String value) {
        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("app.calendar.google-credential.key is not valid base64");
        }
    }
}
