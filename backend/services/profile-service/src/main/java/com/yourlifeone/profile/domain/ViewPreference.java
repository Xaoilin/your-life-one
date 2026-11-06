package com.yourlifeone.profile.domain;

import java.time.Instant;

/** One server-confirmed presentation choice. */
public record ViewPreference(String key, String value, Instant updatedAt) {
}
