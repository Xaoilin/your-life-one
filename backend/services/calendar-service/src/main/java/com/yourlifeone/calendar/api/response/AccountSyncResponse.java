package com.yourlifeone.calendar.api.response;

import java.time.Instant;

/** @param status synced, failed, needs_reconnect or revoked */
public record AccountSyncResponse(
        String accountId, String email, String status, String message, Instant syncedAt, int eventCount) {
}
