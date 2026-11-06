package com.yourlifeone.profile.activity.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.profile.activity.api.command.IngestActivityEventsCommand;
import com.yourlifeone.profile.activity.api.command.IngestActivityEventsHandler;
import com.yourlifeone.profile.activity.api.mapper.ActivityMapper;
import com.yourlifeone.profile.activity.api.request.ActivityEventBatchRequest;
import com.yourlifeone.profile.activity.api.response.ActivityIngestReceiptResponse;
import com.yourlifeone.profile.activity.api.response.ActivityInsightsResponse;
import com.yourlifeone.profile.activity.application.service.ActivityFiltersResolver;
import com.yourlifeone.profile.activity.application.service.ActivityInsightsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The React app's content-free product-usage events and the Activity page's insights over them. */
@RestController
@RequestMapping("/api/profile/v1/activity")
@RequiredArgsConstructor
public class ActivityController {
    private final WriteCommandGateway commands;
    private final ActivityFiltersResolver filters;
    private final ActivityInsightsService insights;
    private final ActivityMapper mapper;

    /** Stores the batch; an event already stored counts as a duplicate, never an error. */
    @PostMapping("/events")
    public ResponseEntity<ActivityIngestReceiptResponse> ingest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ActivityEventBatchRequest request) {
        var command = new IngestActivityEventsCommand(request.events());
        return WriteCommandResponses.of(commands.send(IngestActivityEventsHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, command, ActivityIngestReceiptResponse.class));
    }

    /** A missing filter, or {@code all}, matches every event; {@code rangeDays} defaults to the configured window. */
    @GetMapping("/insights")
    public ActivityInsightsResponse insights(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer rangeDays,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String surface,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String feature) {
        var chosen = filters.resolve(rangeDays, kind, surface, outcome, feature);
        return mapper.toResponse(insights.insights(AuthenticatedUser.id(jwt), chosen));
    }
}
