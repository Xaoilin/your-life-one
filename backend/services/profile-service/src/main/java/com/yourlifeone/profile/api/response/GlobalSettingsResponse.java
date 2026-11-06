package com.yourlifeone.profile.api.response;

import java.time.Instant;

public record GlobalSettingsResponse(String city, String country, String timeZone, Instant updatedAt) {
}
