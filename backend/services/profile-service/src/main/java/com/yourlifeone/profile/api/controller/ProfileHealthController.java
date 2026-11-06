package com.yourlifeone.profile.api.controller;

import com.yourlifeone.profile.api.response.ProfileDatabaseHealthResponse;
import com.yourlifeone.profile.api.response.ProfileHealthResponse;
import com.yourlifeone.profile.infrastructure.database.ProfileDatabaseHealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/health")
@RequiredArgsConstructor
public class ProfileHealthController {
    private static final String SERVICE = "profile-service";

    private final ProfileDatabaseHealthCheck databaseHealthCheck;

    @GetMapping
    public ProfileHealthResponse health() {
        return new ProfileHealthResponse("UP", SERVICE);
    }

    @GetMapping("/database")
    public ResponseEntity<ProfileDatabaseHealthResponse> database() {
        var up = databaseHealthCheck.isDatabaseAvailable();
        var status = up ? "UP" : "DOWN";
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ProfileDatabaseHealthResponse(status, SERVICE, status));
    }
}
