package com.yourlifeone.access.identity.api.controller;

import com.yourlifeone.access.identity.infrastructure.persistence.IdentityDatabaseHealth;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IdentityHealthController {
    private final IdentityDatabaseHealth database;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/health/database")
    public Map<String, String> databaseHealth() {
        database.check();
        return Map.of("status", "UP", "database", "UP");
    }
}
