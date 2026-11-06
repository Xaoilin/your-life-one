package com.yourlifeone.profile.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed and default app preferences ({@code app.profile.preferences.*}).
 *
 * @param themes themes a user may choose
 * @param defaultTheme theme until a user saves preferences; one of {@code themes}
 * @param calendarTabs calendar views a user may open by default
 * @param minDataRetentionDays shortest data retention a user may choose
 * @param maxDataRetentionDays longest data retention a user may choose
 * @param defaultDataRetentionDays retention until a user saves preferences
 * @param maxGoalTags most goal tags a user may keep
 * @param maxGoalTagLength longest goal tag, in characters
 */
@ConfigurationProperties("app.profile.preferences")
public record PreferencesProperties(
        List<String> themes,
        String defaultTheme,
        List<String> calendarTabs,
        int minDataRetentionDays,
        int maxDataRetentionDays,
        int defaultDataRetentionDays,
        int maxGoalTags,
        int maxGoalTagLength) {
    public PreferencesProperties {
        themes = themes == null ? List.of() : List.copyOf(themes);
        calendarTabs = calendarTabs == null ? List.of() : List.copyOf(calendarTabs);
        if (!themes.contains(defaultTheme)) {
            throw new IllegalArgumentException("app.profile.preferences.default-theme must be one of the themes");
        }
        if (defaultDataRetentionDays < minDataRetentionDays || defaultDataRetentionDays > maxDataRetentionDays) {
            throw new IllegalArgumentException(
                    "app.profile.preferences.default-data-retention-days must be within the allowed range");
        }
    }
}
