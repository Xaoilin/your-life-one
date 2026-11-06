package com.yourlifeone.calendar.application.model;

import java.time.Instant;
import lombok.Builder;

/**
 * The outcome of mirroring one Google account.
 *
 * @param status synced, needs_reconnect, revoked or failed
 * @param message why the sync did not complete; null when it did
 * @param eventCount events mirrored in the sync window
 */
@Builder
public record AccountSyncResult(
        String accountId, String email, String status, String message, Instant syncedAt, int eventCount) {
}
