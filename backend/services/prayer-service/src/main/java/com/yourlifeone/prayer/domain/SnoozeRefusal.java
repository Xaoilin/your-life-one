package com.yourlifeone.prayer.domain;

/** Why a reminder cannot be snoozed. */
public enum SnoozeRefusal {
    /** It already used its one snooze. */
    ALREADY_USED,
    /** The snooze would last until the reminder expires (or beyond). */
    TOO_LATE
}
