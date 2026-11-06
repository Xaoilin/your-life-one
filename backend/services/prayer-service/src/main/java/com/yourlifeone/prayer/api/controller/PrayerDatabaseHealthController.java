package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.prayer.api.response.PrayerDatabaseHealthResponse;
import com.yourlifeone.prayer.application.port.PrayerDatabaseHealthCheck;
import com.yourlifeone.commonservice.exception.DatabaseHealthCheckException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prayer/health")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PrayerDatabaseHealthController {
    private static final Logger logger = LoggerFactory.getLogger(PrayerDatabaseHealthController.class);

    private final PrayerDatabaseHealthCheck databaseHealthCheck;

    @GetMapping("/database")
    public ResponseEntity<PrayerDatabaseHealthResponse> databaseHealth() {
        boolean databaseAvailable;
        try {
            databaseAvailable = databaseHealthCheck.isDatabaseAvailable();
        } catch (DatabaseHealthCheckException exception) {
            logHealthCheckException(exception);
            databaseAvailable = false;
        } catch (RuntimeException exception) {
            logger.warn("Prayer database health check failed ({})", exception.getClass().getSimpleName());
            databaseAvailable = false;
        }

        var status = databaseAvailable ? "UP" : "DOWN";
        var response = new PrayerDatabaseHealthResponse(status, "prayer-service", status);
        return ResponseEntity.status(databaseAvailable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    private static void logHealthCheckException(DatabaseHealthCheckException exception) {
        var exceptionType = exception.getClass().getSimpleName();
        if (exception.getCause() == null) {
            logger.warn("Prayer database health check failed: exceptionType={} message={}",
                    exceptionType, exception.getMessage());
        } else {
            logger.error("Prayer database health check failed: exceptionType={} message={}",
                    exceptionType, exception.getMessage(), exception);
        }
    }
}
