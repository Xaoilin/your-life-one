package com.yourlifeone.commonservice.command;

import com.yourlifeone.commonservice.web.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdempotencyKeysTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Test
    void theSameKeyForTheSameUserIsTheSameMessage() {
        assertEquals(IdempotencyKeys.messageId(USER, "create:Fajr"), IdempotencyKeys.messageId(USER, "create:Fajr"));
    }

    @Test
    void keysAreScopedToTheirUser() {
        var other = UUID.fromString("22222222-2222-4222-8222-222222222222");
        assertNotEquals(IdempotencyKeys.messageId(USER, "k"), IdempotencyKeys.messageId(other, "k"));
    }

    @Test
    void differentKeysAreDifferentMessages() {
        assertNotEquals(IdempotencyKeys.messageId(USER, "a"), IdempotencyKeys.messageId(USER, "b"));
    }

    @Test
    void theFingerprintChangesWithTheTypeOrPayload() {
        var payload = JSON.readTree("{\"status\":\"on_time\"}");
        var fingerprint = IdempotencyKeys.fingerprint("create-outcome", payload);
        assertEquals(fingerprint, IdempotencyKeys.fingerprint("create-outcome", JSON.readTree("{\"status\":\"on_time\"}")));
        assertNotEquals(fingerprint, IdempotencyKeys.fingerprint("correct-outcome", payload));
        assertNotEquals(fingerprint, IdempotencyKeys.fingerprint("create-outcome", JSON.readTree("{\"status\":\"late\"}")));
    }

    @Test
    void acceptsVisibleAsciiUpToTwoHundredCharacters() {
        assertEquals("x".repeat(200), IdempotencyKeys.validated("x".repeat(200)));
        assertEquals("prayer:2026-09-26:Fajr:on_time", IdempotencyKeys.validated("prayer:2026-09-26:Fajr:on_time"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"has space", "tab\there", "ünicode"})
    void refusesMissingBlankOrNonAsciiKeys(String key) {
        var error = assertThrows(ApiException.class, () -> IdempotencyKeys.validated(key));
        assertEquals("invalid_idempotency_key", error.code());
    }

    @Test
    void refusesKeysOverTwoHundredCharacters() {
        assertThrows(ApiException.class, () -> IdempotencyKeys.validated("x".repeat(201)));
    }
}
