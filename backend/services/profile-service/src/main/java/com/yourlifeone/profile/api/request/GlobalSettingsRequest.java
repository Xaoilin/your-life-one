package com.yourlifeone.profile.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param timeZone IANA zone, or null or blank to follow the device */
public record GlobalSettingsRequest(
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country,
        @Size(max = 64) String timeZone) {
}
