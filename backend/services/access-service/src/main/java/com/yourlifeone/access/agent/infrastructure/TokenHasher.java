package com.yourlifeone.access.agent.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Turns a token into the form the database keeps: {@code {sha256}} followed by the URL-safe Base64 of the token's
 * SHA-256 digest. A plain digest is enough because every token this service issues is random with at least 96 bits, so
 * a stolen digest cannot be turned back into the token by guessing; a keyed hash would add a secret to store and
 * rotate without making that guess any harder. The prefix tells a digest from a token, so a value that is already a
 * digest is never hashed a second time.
 */
public class TokenHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final String PREFIX = "{sha256}";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** The form of {@code tokenValue} that is stored and compared. The same token always gives the same result. */
    public String hash(String tokenValue) {
        return PREFIX + ENCODER.encodeToString(digest(tokenValue));
    }

    /** Whether {@code storedValue} is already a digest made by {@link #hash(String)} rather than a token. */
    public boolean isHashed(String storedValue) {
        return storedValue.startsWith(PREFIX);
    }

    private static byte[] digest(String tokenValue) {
        try {
            return MessageDigest.getInstance(ALGORITHM).digest(tokenValue.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            // The Java platform must provide SHA-256.
            throw new IllegalStateException(impossible);
        }
    }
}
