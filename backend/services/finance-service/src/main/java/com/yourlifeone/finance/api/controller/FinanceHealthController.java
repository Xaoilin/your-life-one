package com.yourlifeone.finance.api.controller;

import com.yourlifeone.finance.api.response.FinanceDatabaseHealthResponse;
import com.yourlifeone.finance.api.response.FinanceHealthResponse;
import com.yourlifeone.finance.infrastructure.database.FinanceDatabaseHealthCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/health")
@RequiredArgsConstructor
public class FinanceHealthController {
    private static final String SERVICE = "finance-service";

    private final FinanceDatabaseHealthCheck databaseHealthCheck;

    @GetMapping
    public FinanceHealthResponse health() {
        return new FinanceHealthResponse("UP", SERVICE);
    }

    @GetMapping("/database")
    public ResponseEntity<FinanceDatabaseHealthResponse> database() {
        var up = databaseHealthCheck.isDatabaseAvailable();
        var status = up ? "UP" : "DOWN";
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FinanceDatabaseHealthResponse(status, SERVICE, status));
    }
}
