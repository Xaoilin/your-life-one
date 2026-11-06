package com.yourlifeone.life.api.controller;

import com.yourlifeone.life.api.response.LifeDatabaseHealthResponse;
import com.yourlifeone.life.api.response.LifeHealthResponse;
import com.yourlifeone.life.infrastructure.database.LifeDatabaseHealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/life/health")
@RequiredArgsConstructor
public class LifeHealthController {
    private static final String SERVICE = "life-service";

    private final LifeDatabaseHealthCheck databaseHealthCheck;

    @GetMapping
    public LifeHealthResponse health() {
        return new LifeHealthResponse("UP", SERVICE);
    }

    @GetMapping("/database")
    public ResponseEntity<LifeDatabaseHealthResponse> database() {
        var up = databaseHealthCheck.isDatabaseAvailable();
        var status = up ? "UP" : "DOWN";
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new LifeDatabaseHealthResponse(status, SERVICE, status));
    }
}
