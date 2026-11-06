package com.yourlifeone.access.identity.api.response;

import java.time.Instant;

public record SessionResponse(SessionUser user, String accessToken, Instant expiresAt) {
}
