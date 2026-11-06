package com.yourlifeone.planner.api.controller;

import com.yourlifeone.planner.api.response.PlannerDatabaseHealthResponse;
import com.yourlifeone.planner.api.response.PlannerHealthResponse;
import com.yourlifeone.planner.infrastructure.database.PlannerDatabaseHealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planner/health")
@RequiredArgsConstructor
public class PlannerHealthController {
    private static final String SERVICE = "planner-service";

    private final PlannerDatabaseHealthCheck databaseHealthCheck;

    @GetMapping
    public PlannerHealthResponse health() {
        return new PlannerHealthResponse("UP", SERVICE);
    }

    @GetMapping("/database")
    public ResponseEntity<PlannerDatabaseHealthResponse> database() {
        var up = databaseHealthCheck.isDatabaseAvailable();
        var status = up ? "UP" : "DOWN";
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new PlannerDatabaseHealthResponse(status, SERVICE, status));
    }
}
