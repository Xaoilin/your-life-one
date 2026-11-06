package com.yourlifeone.profile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Profile service settings ({@code app.profile.*}).
 *
 * @param defaultCity city shown until a user saves settings
 * @param defaultCountry country shown until a user saves settings
 */
@ConfigurationProperties("app.profile")
public record ProfileProperties(String defaultCity, String defaultCountry) {
}
