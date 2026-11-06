package com.yourlifeone.prayer.api.response;

public record PreferencesResponse(boolean enabled, boolean reminderEnabled, int reminderMinutes) {
}
