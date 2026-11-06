package com.yourlifeone.calendar.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A calendar account: one Google identity, or a local account.
 *
 * @param email lower-case Google address; free text (possibly empty) for local accounts
 * @param paletteIndex the colour the app gives the account; null lets the app choose
 */
@Builder(toBuilder = true)
public record CalendarAccount(
        String id,
        AccountProvider provider,
        String name,
        String email,
        boolean primary,
        Integer paletteIndex,
        AuthStatus authStatus,
        String authError,
        Instant lastSyncedAt,
        String syncError) {
    public boolean isGoogle() {
        return provider == AccountProvider.GOOGLE;
    }
}
