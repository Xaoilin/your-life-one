package com.yourlifeone.prayer.api.command;

/** Snoozes one sent reminder, named by its key (from the request path). */
public record SnoozeReminderCommand(String key) {
}
