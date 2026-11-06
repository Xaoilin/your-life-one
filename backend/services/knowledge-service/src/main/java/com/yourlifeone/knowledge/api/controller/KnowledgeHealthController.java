package com.yourlifeone.knowledge.api.controller;

import com.yourlifeone.knowledge.api.response.KnowledgeDatabaseHealthResponse;
import com.yourlifeone.knowledge.api.response.KnowledgeHealthResponse;
import com.yourlifeone.knowledge.infrastructure.database.KnowledgeDatabaseHealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge/health")
@RequiredArgsConstructor
public class KnowledgeHealthController {
    private static final String SERVICE = "knowledge-service";

    private final KnowledgeDatabaseHealthCheck databaseHealthCheck;

    @GetMapping
    public KnowledgeHealthResponse health() {
        return new KnowledgeHealthResponse("UP", SERVICE);
    }

    @GetMapping("/database")
    public ResponseEntity<KnowledgeDatabaseHealthResponse> database() {
        var up = databaseHealthCheck.isDatabaseAvailable();
        var status = up ? "UP" : "DOWN";
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new KnowledgeDatabaseHealthResponse(status, SERVICE, status));
    }
}
