package com.yourlifeone.profile.api.controller;

import com.yourlifeone.profile.api.mapper.OperationalEventMapper;
import com.yourlifeone.profile.api.request.OperationalEventBatchRequest;
import com.yourlifeone.profile.api.response.OperationalEventReceiptResponse;
import com.yourlifeone.profile.application.service.OperationalEventRecorder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Collects the React app's operational events as metrics for Grafana. This does not write account data
 * or enter the command queue. A bounded in-memory cache suppresses retried event IDs.
 */
@RestController
@RequestMapping("/api/profile/v1/operational-events")
@RequiredArgsConstructor
public class OperationalEventController {
    static final int SCHEMA_VERSION = 1;

    private final OperationalEventRecorder recorder;
    private final OperationalEventMapper mapper;

    @PostMapping
    public OperationalEventReceiptResponse record(@Valid @RequestBody OperationalEventBatchRequest batch) {
        var accepted = recorder.record(mapper.toDomain(batch.events()));
        return new OperationalEventReceiptResponse(true, accepted, SCHEMA_VERSION);
    }
}
