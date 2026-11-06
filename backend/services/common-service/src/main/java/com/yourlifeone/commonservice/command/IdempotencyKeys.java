package com.yourlifeone.commonservice.command;

import com.yourlifeone.commonservice.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/** Rules for the {@code Idempotency-Key} request header. */
public final class IdempotencyKeys {
    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";
    private static final Pattern VALID_KEY = Pattern.compile("[\\x21-\\x7E]{1,200}");

    private IdempotencyKeys() {
    }

    /** A key is 1-200 visible ASCII characters. */
    public static String validated(String key) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw ApiException.badRequest("invalid_idempotency_key",
                    "Idempotency-Key must be 1-200 visible ASCII characters.");
        }
        return key;
    }

    /** Keys are scoped to their user, so two users can never collide. */
    public static UUID messageId(UUID userId, String key) {
        return UUID.nameUUIDFromBytes(("user:" + userId + "\nkey:" + key).getBytes(StandardCharsets.UTF_8));
    }

    public static String fingerprint(String type, JsonNode payload) {
        return sha256(type + "\n" + payload);
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
