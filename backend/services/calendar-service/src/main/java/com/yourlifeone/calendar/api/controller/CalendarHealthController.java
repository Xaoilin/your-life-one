package com.yourlifeone.calendar.api.controller;

import com.yourlifeone.calendar.api.response.CalendarDatabaseHealthResponse;
import com.yourlifeone.calendar.api.response.CalendarHealthResponse;
import com.yourlifeone.calendar.infrastructure.database.CalendarDatabaseHealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar/health")
@RequiredArgsConstructor
public class CalendarHealthController {
    private static final String SERVICE = "calendar-service";

    private final CalendarDatabaseHealthCheck databaseHealthCheck;

    @GetMapping
    public CalendarHealthResponse health() {
        return new CalendarHealthResponse("UP", SERVICE);
    }

    @GetMapping("/database")
    public ResponseEntity<CalendarDatabaseHealthResponse> database() {
        var up = databaseHealthCheck.isDatabaseAvailable();
        var status = up ? "UP" : "DOWN";
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new CalendarDatabaseHealthResponse(status, SERVICE, status));
    }
}
