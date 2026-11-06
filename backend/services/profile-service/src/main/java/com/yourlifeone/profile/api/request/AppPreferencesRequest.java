package com.yourlifeone.profile.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The whole app-preferences value; allowed options are checked against configuration.
 *
 * @param defaultCalendarTab null uses the app's own default view
 */
public record AppPreferencesRequest(
        @NotNull @Size(max = 32) String theme,
        @NotNull Integer dataRetentionDays,
        @NotNull Boolean telemetry,
        @Size(max = 32) String defaultCalendarTab,
        @NotNull List<@NotNull @Size(max = 100) String> goalTags) {
}
