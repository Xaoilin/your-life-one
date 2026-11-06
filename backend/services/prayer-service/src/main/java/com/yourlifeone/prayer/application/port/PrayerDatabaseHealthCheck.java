package com.yourlifeone.prayer.application.port;

/** Outbound port used by the HTTP health endpoint to verify database connectivity. */
@FunctionalInterface
public interface PrayerDatabaseHealthCheck {
    boolean isDatabaseAvailable();
}
