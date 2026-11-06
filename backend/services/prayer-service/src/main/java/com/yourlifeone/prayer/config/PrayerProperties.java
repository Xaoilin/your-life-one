package com.yourlifeone.prayer.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Prayer service settings ({@code app.prayer.*}).
 *
 * @param theShiaBaseUrl TheShia public API root
 * @param geocodingBaseUrl Open-Meteo geocoding API root
 * @param providerUserAgent identifying agent sent to public providers
 * @param reminderMinutesOptions reminder lead times a user may choose, in minutes
 * @param defaultReminderMinutes reminder lead time for new users; must be one of the options
 * @param reminderCheckInterval how often due deadline reminders are looked for and sent
 */
@ConfigurationProperties("app.prayer")
public record PrayerProperties(
        String theShiaBaseUrl, String geocodingBaseUrl, String providerUserAgent,
        List<Integer> reminderMinutesOptions, int defaultReminderMinutes,
        Duration reminderCheckInterval) {
    public PrayerProperties {
        reminderMinutesOptions = reminderMinutesOptions == null ? List.of() : List.copyOf(reminderMinutesOptions);
        reminderCheckInterval = reminderCheckInterval == null ? Duration.ofSeconds(30) : reminderCheckInterval;
        if (!reminderMinutesOptions.contains(defaultReminderMinutes)) {
            throw new IllegalArgumentException("app.prayer.default-reminder-minutes must be one of "
                    + "app.prayer.reminder-minutes-options");
        }
    }
}
