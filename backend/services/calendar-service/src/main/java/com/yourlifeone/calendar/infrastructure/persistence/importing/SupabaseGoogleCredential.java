package com.yourlifeone.calendar.infrastructure.persistence.importing;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/**
 * A credential row of the Supabase {@code public.google_calendar_credentials} table, already bounded
 * to fit {@code calendar.google_credential}.
 */
@Builder
public record SupabaseGoogleCredential(
        UUID userId,
        String email,
        String refreshToken,
        String scope,
        OffsetDateTime lastRefreshAt,
        String failureReason,
        OffsetDateTime failureAt,
        OffsetDateTime createdAt) {
    /** Never print the token. */
    @Override
    public String toString() {
        return "SupabaseGoogleCredential[userId=" + userId + "]";
    }
}
