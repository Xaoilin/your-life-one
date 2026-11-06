package com.yourlifeone.profile.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * Account-wide settings shared by every Your Life One page.
 *
 * @param timeZone IANA zone the app displays times in; null follows the device
 * @param updatedAt null until the user first saves settings
 */
@Builder(toBuilder = true)
public record GlobalSettings(String city, String country, String timeZone, Instant updatedAt) {
}
