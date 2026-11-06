package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.prayer.api.response.PrayerDatabaseHealthResponse;
import com.yourlifeone.prayer.application.port.PrayerDatabaseHealthCheck;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrayerDatabaseHealthControllerTest {
    @Test
    void returnsUpWhenTheDatabaseQuerySucceeds() {
        var controller = new PrayerDatabaseHealthController(() -> true);

        var response = controller.databaseHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PrayerDatabaseHealthResponse("UP", "prayer-service", "UP"), response.getBody());
    }

    @Test
    void returnsServiceUnavailableWhenTheDatabaseIsDown() {
        PrayerDatabaseHealthCheck down = () -> false;
        var controller = new PrayerDatabaseHealthController(down);

        var response = controller.databaseHealth();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(new PrayerDatabaseHealthResponse("DOWN", "prayer-service", "DOWN"), response.getBody());
    }

    @Test
    void hidesUnexpectedDatabaseFailuresFromTheResponse() {
        var controller = new PrayerDatabaseHealthController(() -> {
            throw new IllegalStateException("private connection detail");
        });

        var response = controller.databaseHealth();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(new PrayerDatabaseHealthResponse("DOWN", "prayer-service", "DOWN"), response.getBody());
    }
}
