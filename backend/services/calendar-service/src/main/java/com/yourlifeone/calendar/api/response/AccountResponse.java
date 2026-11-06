package com.yourlifeone.calendar.api.response;

import java.time.Instant;

/**
 * @param provider google or local
 * @param authStatus connected, needs_reconnect, revoked or error
 */
public record AccountResponse(
        String id, String provider, String name, String email, boolean isPrimary, Integer paletteIndex,
        String authStatus, String authError, Instant lastSyncedAt, String syncError) {
}
