package com.yourlifeone.profile.api.command;

import java.time.Instant;

/** A user's connection record for one provider, as queued. */
public record SaveIntegrationCommand(String provider, String status, Instant configuredAt, String lastError) {
}
