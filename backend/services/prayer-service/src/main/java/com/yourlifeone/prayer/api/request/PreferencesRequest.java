package com.yourlifeone.prayer.api.request;

import jakarta.validation.constraints.NotNull;

public record PreferencesRequest(@NotNull Boolean enabled, @NotNull Boolean reminderEnabled, @NotNull Integer reminderMinutes) {
}
